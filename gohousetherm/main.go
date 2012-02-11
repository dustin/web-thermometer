package main

import (
	"encoding/json"
	"image"
	"log"
	"math"
	"os"

	// Yay side-effects
	_ "image/gif"
	_ "image/png"
)

var houseBase image.Image
var thermImage image.Image

var conf houseConfig
var bySerial map[string]*room

func loadImage(name string) image.Image {
	f, err := os.Open(name)
	if err != nil {
		log.Fatal(err)
	}
	defer f.Close()
	i, _, err := image.Decode(f)
	if err != nil {
		log.Fatal(err)
	}
	return i
}

func loadConfig() {
	f, err := os.Open("house.json")
	if err != nil {
		log.Fatal(err)
	}
	defer f.Close()
	err = json.NewDecoder(f).Decode(&conf)
	if err != nil {
		log.Fatal(err)
	}

	bySerial = make(map[string]*room)
	for k, r := range conf.Rooms {
		sn := r.SN
		if sn == "" {
			sn = k
		}
		bySerial[sn] = r
		r.latest = math.NaN()
	}
}

func main() {
	houseBase = loadImage("house.png")
	thermImage = loadImage("therm-c.gif")
	loadConfig()

	err := readNet()
	if err != nil {
		log.Fatalf("Error reading the net:  %v", err)
	}

	addr := ":7777"
	serveWeb(addr)
}
