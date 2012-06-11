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

	"code.google.com/p/draw2d/draw2d"
	"code.google.com/p/freetype-go/freetype"
	"code.google.com/p/freetype-go/freetype/truetype"

	"github.com/dustin/web-thermometer/gohousetherm/houseconf"
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

func drawBox(i *image.NRGBA, room *houseconf.Room) {
	draw.Draw(i, image.Rect(room.Rect.X-1, room.Rect.Y-1,
		room.Rect.X+room.Rect.W+1,
		room.Rect.Y+room.Rect.H+1),
		image.NewUniform(color.Black),
		image.Pt(room.Rect.X-1, room.Rect.Y-1),
		draw.Over)
}

func ifZero(a, b int) int {
	if a == 0 {
		return b
	}
	return a
}

func getFillColor(room *houseconf.Room, reading, relevance float64) (rv color.NRGBA) {
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
	return
}

func fillGradient(img *image.NRGBA, room *houseconf.Room, reading float64) {
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

func fillSolid(i *image.NRGBA, room *houseconf.Room, c color.Color) {
	draw.Draw(i, image.Rect(room.Rect.X, room.Rect.Y,
		room.Rect.X+room.Rect.W,
		room.Rect.Y+room.Rect.H),
		image.NewUniform(c),
		image.Pt(room.Rect.X, room.Rect.Y),
		draw.Over)
}

func fill(i *image.NRGBA, room *houseconf.Room, reading float64) {
	switch {
	case reading < room.Min:
		fillSolid(i, room, color.NRGBA{0, 0, 255, 255})
	case reading > room.Max:
		fillSolid(i, room, color.NRGBA{255, 0, 0, 255})
	default:
		fillGradient(i, room, reading)
	}
}

func drawLabel(i draw.Image, room *houseconf.Room, lbl string) {
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

func drawSparklines(i *image.NRGBA, room *houseconf.Room, roomReadings []reading) {
	// Not interested in plotting fewer than two points
	if len(roomReadings) < 2 {
		return
	}

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

	for pos, r := range roomReadings {
		x := len(roomReadings) - pos + sparkx - 1
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

func drawHouse() image.Image {
	i := image.NewNRGBA(houseBase.Bounds())

	alldata := readingsSingleton.getReadings()

	draw.Draw(i, houseBase.Bounds(), houseBase, image.Pt(0, 0), draw.Over)

	for _, roomName := range conf.Colorize {
		room := conf.Rooms[roomName]
		drawBox(i, room)
		roomReadings, ok := alldata[room.SN]
		if ok {
			reading := roomReadings[0].reading
			fill(i, room, reading)
			drawLabel(i, room, fmt.Sprintf("%.2f", reading))
			drawSparklines(i, room, roomReadings)
		} else {
			fillSolid(i, room, color.White)
			drawLabel(i, room, "??.??")
		}
	}

	return i
}

func houseServer(w http.ResponseWriter, req *http.Request) {
	w.Header().Set("Content-Type", "image/png")

	i := drawHouse()

	png.Encode(w, i)
}

func drawTemp(i draw.Image, r reading) {
	x1, y1 := float64(66), float64(65)

	// Translate the angle because we're a little crooked
	trans := -90.0

	// Calculate the angle based on the temperature
	angle := (r.reading * 1.8) + trans
	// Calculate the angle in radians
	rad := ((angle / 360.0) * 2.0 * 3.14159265358979)
	// Find the extra points
	x2 := math.Sin(rad) * 39.0
	y2 := math.Cos(rad) * 39.0
	// Negate the y, we're upside-down
	y2 = -y2
	// Move over to the starting points.
	x2 += x1
	y2 += y1

	// Draw the line...
	gc := draw2d.NewGraphicContext(i)
	gc.MoveTo(x1, y1)
	gc.LineTo(x2, y2)
	gc.Stroke()

	// And label it
	c := freetype.NewContext()
	c.SetDPI(72)
	c.SetFont(font)
	c.SetFontSize(10)
	c.SetClip(i.Bounds())
	c.SetDst(i)
	c.SetSrc(image.Black)

	pt := freetype.Pt(52, 72+c.FUnitToPixelRU(font.UnitsPerEm()))
	c.DrawString(fmt.Sprintf("%.2f", r.reading), pt)

}

func thermServer(w http.ResponseWriter, req *http.Request) {
	w.Header().Set("Content-Type", "image/png")

	i := image.NewRGBA(thermImage.Bounds())

	draw.Draw(i, thermImage.Bounds(), thermImage, image.Pt(0, 0), draw.Over)

	p := req.URL.Path[7:]
	r, ok := readingsSingleton.getCurrent(p)

	if ok {
		drawTemp(i, r)
	}

	png.Encode(w, i)
}

func serveWeb(addr string) {
	http.HandleFunc("/", houseServer)
	http.HandleFunc("/therm/", thermServer)
	log.Printf("Listening to web requests on %s", addr)
	log.Fatal(http.ListenAndServe(addr, nil))
}
