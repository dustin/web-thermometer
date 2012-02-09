package main

import (
	"image"
	"image/color"
	"image/png"
	"math"
	"net/http"
)

func drawBox(i *image.NRGBA, room room) {
	for x := 0; x < room.Rect.W; x++ {
		for y := 0; y < room.Rect.H; y++ {
			i.Set(room.Rect.X+x, room.Rect.Y+y, color.White)
		}
	}
	for x := -1; x < room.Rect.W+2; x++ {
		i.Set(room.Rect.X+x, room.Rect.Y-1, color.Black)
		i.Set(room.Rect.X+x, room.Rect.Y+room.Rect.H+1, color.Black)
	}
	for y := -1; y < room.Rect.H+2; y++ {
		i.Set(room.Rect.X-1, room.Rect.Y+y, color.Black)
		i.Set(room.Rect.X+room.Rect.W+1, room.Rect.Y+y, color.Black)
	}
}

func ifZero(a, b int) int {
	if a == 0 {
		return b
	}
	return a
}

func getFillColor(room room, reading, relevance float64) (rv color.NRGBA) {
	normal := room.Min + ((room.Max - room.Min) / 2.0)
	maxDistance := room.Max - room.Min
	switch {
	case reading < room.Min:
		rv = color.NRGBA{0, 0, 255, 255}
	case reading > room.Max:
		rv = color.NRGBA{255, 0, 0, 255}
	default:
		if reading > normal {
			rv.R = 255
		} else {
			rv.B = 255
		}
		rv.A = 255
		distance := math.Abs(reading - normal)
		distancePercent := distance / maxDistance

		factor := math.Max(distancePercent, (1.0 - relevance))

		h, _, _ := RGB2HSL(rv)
		brightness := 0.9 + (factor / 10.0)
		rv = HSL2RGB(h, relevance*distancePercent, brightness)
	}
	return
}

func fillGradient(img *image.NRGBA, room room, reading float64) {
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

func houseServer(w http.ResponseWriter, req *http.Request) {
	w.Header().Set("Content-Type", "image/png")

	i := image.NewNRGBA(houseBase.Bounds())

	// draw.Draw(i, houseBase.Bounds(), houseBase, image.Pt(0, 0), draw.Over)

	for _, roomName := range conf.Colorize {
		room := conf.Rooms[roomName]
		drawBox(i, room)
		fillGradient(i, room, 22.0)
	}

	png.Encode(w, i)
}
