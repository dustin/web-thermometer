package main

import (
	"image"
	"testing"
	"time"

	"github.com/dustin/web-thermometer/gohousetherm/houseconf"
)

func BenchmarkTherm(b *testing.B) {
	t := time.Now()
	for i := 0; i < b.N; i++ {
		b.StopTimer()
		img := image.NewRGBA(image.Rect(0, 0, 133, 132))
		b.StartTimer()

		drawTemp(img, reading{t, "x", 27.33})
	}
}

func BenchmarkNewThermImage(b *testing.B) {
	for i := 0; i < b.N; i++ {
		image.NewRGBA(image.Rect(0, 0, 133, 132))
	}
}

func BenchmarkHouse(b *testing.B) {
	b.StopTimer()
	houseBase = loadImage("house.png")
	readingsSingleton = newReadingServer()
	go processReadings()
	var err error
	conf, err = houseconf.LoadConfig("houseconf/house.json")
	if err != nil {
		b.Fatalf("Error loading config: %v", err)
	}

	for k, r := range conf.Rooms {
		name := r.SN
		if name == "" {
			name = k
		}
		readingsSingleton.newReading(reading{time.Now(),
			name,
			27.73,
		})
	}

	b.StartTimer()

	for i := 0; i < b.N; i++ {
		drawHouse()
	}
}
