package houseconf

import (
	"encoding/json"
	"math"
	"os"
)

// Room definition.
type Room struct {
	SN   string
	Max  float64
	Min  float64
	Rect struct {
		H int
		W int
		X int
		Y int
	}
	Therm struct {
		X int
		Y int
	}
	Spark struct {
		X int
		Y int
		W int
		H int
	}
	Reading struct {
		X int
		Y int
	}

	Latest float64

	Name string
}

// Width of a sparkline defined for this room.
func (r *Room) SparkWidth() int {
	if r.Spark.W != 0 {
		return r.Spark.W
	}
	return r.Rect.W
}

// Configuration for all the house things.
type HouseConfig struct {
	Dims struct {
		H int
		W int
	}
	MaxRelevantDistance float64
	Rooms               map[string]*Room
	bySerial            map[string]*Room
	Colorize            []string
}

// Get the name for this Serial Number
func (hc *HouseConfig) NameOf(sn string) string {
	r := hc.BySerial(sn)
	if r == nil {
		return sn
	}
	return r.Name
}

// Get the room by serial number.
func (hc *HouseConfig) BySerial(sn string) *Room {
	return hc.bySerial[sn]
}

// Load config from a JSON file.
func LoadConfig(path string) (conf HouseConfig, err error) {
	f, err := os.Open(path)
	if err != nil {
		return conf, err
	}
	defer f.Close()

	err = json.NewDecoder(f).Decode(&conf)
	if err != nil {
		return
	}

	conf.bySerial = make(map[string]*Room)
	for k, r := range conf.Rooms {
		sn := r.SN
		if sn == "" {
			sn = k
		}
		conf.bySerial[sn] = r
		r.Latest = math.NaN()
		r.Name = k
	}
	return
}
