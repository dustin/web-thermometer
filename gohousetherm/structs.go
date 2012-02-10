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

func newReadings() (rv readings) {
	rv.current = make(map[string]reading)
	rv.previous = make(map[string]*ring.Ring)
	rv.input = make(chan reading, 1000)
	rv.req = make(chan chan response)
	return
}

type readings struct {
	current  map[string]reading
	previous map[string]*ring.Ring

	input chan reading
	req   chan chan response
}

func (rs *readings) getReadings() response {
	ch := make(chan response)
	rs.req <- ch
	return <-ch
}
