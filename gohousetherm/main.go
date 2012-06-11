package main

import (
	"flag"
	"image"
	"log"
	"os"

	// Yay side-effects
	_ "image/gif"
	_ "image/png"

	"github.com/dustin/web-thermometer/gohousetherm/houseconf"
)

var couchURL = flag.String("couch", "", "URL of the CouchDB")

var houseBase image.Image
var thermImage image.Image

var conf houseconf.HouseConfig

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

func main() {
	flag.Parse()
	var err error

	houseBase = loadImage("house.png")
	thermImage = loadImage("therm-c.gif")
	conf, err = houseconf.LoadConfig("houseconf/house.json")

	if err != nil {
		log.Fatalf("Error reading config: %v", err)
	}

	err = readNet()
	if err != nil {
		log.Fatalf("Error reading the net:  %v", err)
	}

	if *couchURL != "" {
		readingsSingleton.register(couchLoop())
	}

	addr := ":7777"
	serveWeb(addr)
}
