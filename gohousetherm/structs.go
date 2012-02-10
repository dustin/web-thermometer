package main

type room struct {
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
	}
	Reading struct {
		X int
		Y int
	}
	latest float64
}

type houseConfig struct {
	Dims struct {
		H int
		W int
	}
	MaxRelevantDistance float64
	Rooms               map[string]*room
	Colorize            []string
}