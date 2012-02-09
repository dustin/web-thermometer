package main

import (
	"encoding/json"
	"image"
	"image/png"
	"log"
	"net/http"
	"os"
)

var houseBase image.Image
var conf houseConfig

func loadBaseImage() {
	f, err := os.Open("house.png")
	if err != nil {
		log.Fatal(err)
	}
	defer f.Close()
	i, err := png.Decode(f)
	if err != nil {
		log.Fatal(err)
	}
	houseBase = i
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
}

func main() {
	loadBaseImage()
	loadConfig()

	addr := ":7777"
	http.HandleFunc("/", houseServer)
	log.Printf("Listening to web requests on %s", addr)
	log.Fatal(http.ListenAndServe(addr, nil))
}
