package main

import (
	"fmt"
	"image"
	"image/color"
	"image/draw"
	"image/png"
	"io/ioutil"
	"log"
	"math"
	"net/http"
	"os"
	"path/filepath"

	"code.google.com/p/freetype-go/freetype"
	"code.google.com/p/freetype-go/freetype/truetype"
)

var font *truetype.Font

func init() {
	fontPath := os.Getenv("TEMPWEB_FONT")
	if fontPath == "" {
		goPath := os.Getenv("GOPATH")
		fontPath = filepath.Join(goPath,
			"src/code.google.com/p/freetype-go/luxi-fonts/luximr.ttf")
	}
	fontBytes, err := ioutil.ReadFile(fontPath)
	if err != nil {
		log.Fatalf("Error loading font (try setting TEMPWEB_FONT): %v", err)
	}
	font, err = freetype.ParseFont(fontBytes)
	if err != nil {
		log.Fatalf("Error parsing font: %v", err)
	}
}

func drawBox(i *image.NRGBA, room *room) {
	draw.Draw(i, image.Rect(room.Rect.X-1, room.Rect.Y-1,
		room.Rect.X+room.Rect.W+1,
		room.Rect.Y+room.Rect.H+1),
		image.NewUniform(color.Black),
		image.Pt(room.Rect.X-1, room.Rect.Y-1),
		draw.Over)
	draw.Draw(i, image.Rect(room.Rect.X, room.Rect.Y,
		room.Rect.X+room.Rect.W,
		room.Rect.Y+room.Rect.H),
		image.NewUniform(color.White),
		image.Pt(room.Rect.X, room.Rect.Y),
		draw.Over)
}

func ifZero(a, b int) int {
	if a == 0 {
		return b
	}
	return a
}

func getFillColor(room *room, reading, relevance float64) (rv color.NRGBA) {
	switch {
	case reading < room.Min:
		rv = color.NRGBA{0, 0, 255, 255}
	case reading > room.Max:
		rv = color.NRGBA{255, 0, 0, 255}
	default:
		normal := room.Min + ((room.Max - room.Min) / 2.0)
		maxDifference := room.Max - room.Min

		difference := math.Abs(reading - normal)
		differencePercent := difference / maxDifference

		factor := 1.0 - relevance

		base := 255.0 - (255.0 * differencePercent)
		colorval := uint8(base + ((255 - base) * factor))

		rv.A = 255
		rv.R = uint8(colorval)
		rv.G = rv.R
		rv.B = rv.R
		if reading > normal {
			rv.R = 255
		} else {
			rv.B = 255
		}
	}
	return
}

func fillGradient(img *image.NRGBA, room *room, reading float64) {
	tx := ifZero(room.Therm.X, room.Rect.X+(room.Rect.W/2))
	ty := ifZero(room.Therm.Y, room.Rect.Y+(room.Rect.H/2))

	for i := 0; i < room.Rect.W; i++ {
		for j := 0; j < room.Rect.H; j++ {
			px, py := room.Rect.X+i, room.Rect.Y+j
			xd, yd := float64(px-tx), float64(py-ty)
			distance := math.Sqrt(xd*xd + yd*yd)
			relevance := 1.0 - (distance / conf.MaxRelevantDistance)
			if relevance < 0 {
				relevance = 0
			}

			img.Set(px, py, getFillColor(room, reading, relevance))
		}
	}
}

func drawLabel(i draw.Image, room *room, lbl string) {
	charwidth := 6
	charheight := 12

	x := ifZero(room.Reading.X, (room.Rect.X + (room.Rect.W / 2) -
		((len(lbl) * charwidth) / 2)))
	y := ifZero(room.Reading.Y, (room.Rect.Y +
		((room.Rect.H - charheight*2) / 2) - 12))

	c := freetype.NewContext()
	c.SetDPI(72)
	c.SetFont(font)
	c.SetFontSize(10)
	c.SetClip(image.Rect(x, y, x+room.Rect.W, y+charheight))
	c.SetDst(i)
	c.SetSrc(image.Black)

	pt := freetype.Pt(x, y+c.FUnitToPixelRU(font.UnitsPerEm()))
	c.DrawString(lbl, pt)
}

func drawSparklines(i *image.NRGBA, room *room, roomReadings []reading) {
	// Not interested in plotting fewer than two points
	if len(roomReadings) < 2 {
		return
	}

	sparkw := ifZero(room.Spark.W, room.Rect.W)
	sparkh := ifZero(room.Spark.H, 20)
	sparkx := ifZero(room.Spark.X, room.Rect.X)
	sparky := ifZero(room.Spark.Y, (room.Rect.Y+room.Rect.H)-sparkh)

	high, low := math.SmallestNonzeroFloat64, math.MaxFloat64

	for _, r := range roomReadings {
		high = math.Max(high, r.reading)
		low = math.Min(low, r.reading)
	}

	if (high - low) < float64(sparkh) {
		avg := high - ((high - low) / 2.0)
		low = avg + (float64(sparkh) / 2.0)
		high = avg - (float64(sparkh) / 2.0)
	}

	numPoints := len(roomReadings)
	if numPoints >= sparkw {
		numPoints = sparkw
	}

	for pos, r := range roomReadings {
		x := numPoints - pos + sparkx
		heightPercent := (r.reading - low) / (high - low)
		y := int((float64(sparky) + float64(sparkh)) - (float64(sparkh) * heightPercent))
		if y > sparky+sparkh {
			y = sparky + sparkh
		}
		if y < sparky {
			y = sparky
		}

		i.Set(x, y, color.Gray{127})
	}
}

func houseServer(w http.ResponseWriter, req *http.Request) {
	w.Header().Set("Content-Type", "image/png")

	i := image.NewNRGBA(houseBase.Bounds())

	draw.Draw(i, houseBase.Bounds(), houseBase, image.Pt(0, 0), draw.Over)

	alldata := readingsSingleton.getReadings()

	for _, roomName := range conf.Colorize {
		room := conf.Rooms[roomName]
		drawBox(i, room)
		roomReadings, ok := alldata[room.SN]
		if ok {
			reading := roomReadings[0].reading
			fillGradient(i, room, reading)
			drawLabel(i, room, fmt.Sprintf("%.2f", reading))
			drawSparklines(i, room, roomReadings)
		} else {
			drawLabel(i, room, "??.??")
		}
	}

	png.Encode(w, i)
}
