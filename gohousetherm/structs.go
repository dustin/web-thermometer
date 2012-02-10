package main

import (
	"container/ring"
	"time"
)

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
		W int
		H int
	}
	Reading struct {
		X int
		Y int
	}
	latest float64
}

func (r *room) sparkWidth() int {
	if r.Spark.W != 0 {
		return r.Spark.W
	}
	return r.Rect.W
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

type reading struct {
	when    time.Time
	sensor  string
	reading float64
}

func (r reading) TS() string {
	return r.when.Format(timeOutFormat)
}

type response map[string][]reading

type readingServer struct {
	current  map[string]reading
	previous map[string]*ring.Ring

	input chan reading
	req   chan chan response
}
