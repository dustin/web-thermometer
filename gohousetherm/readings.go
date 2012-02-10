package main

import (
	"container/ring"
)

var readingsSingleton = newReadingServer()

func init() {
	go processReadings()
}

func processReadings() {
	for {
		select {
		case r := <-readingsSingleton.input:
			readingsSingleton.newReading(r)
		case r := <-readingsSingleton.req:
			readingsSingleton.handleRequest(r)
		}
	}
}

func (rs *readingServer) newReading(r reading) {
	rs.current[r.sensor] = r
	rng := rs.previous[r.sensor]
	if rng == nil {
		rng = ring.New(100)
	}
	rng = rng.Prev()
	rng.Value = r
	rs.previous[r.sensor] = rng
}

func (rs *readingServer) handleRequest(ch chan response) {
	response := make(map[string][]reading)
	for k, v := range rs.previous {
		stuff := []reading{}
		v.Do(func(x interface{}) {
			if x != nil {
				stuff = append(stuff, x.(reading))
			}
		})
		response[k] = stuff
	}
	ch <- response
}

func newReadingServer() (rv readingServer) {
	rv.current = make(map[string]reading)
	rv.previous = make(map[string]*ring.Ring)
	rv.input = make(chan reading, 1000)
	rv.req = make(chan chan response)
	return
}

func (rs *readingServer) getReadings() response {
	ch := make(chan response)
	rs.req <- ch
	return <-ch
}
