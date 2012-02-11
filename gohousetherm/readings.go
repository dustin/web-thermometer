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
		rng = ring.New(bySerial[r.sensor].sparkWidth())
	}
	rng = rng.Prev()
	rng.Value = r
	rs.previous[r.sensor] = rng

	for _, ch := range rs.registrations {
		ch <- r
	}
}

func (rs *readingServer) handleRequest(req request) {
	response := make(map[string][]reading)
	for k, v := range rs.previous {
		var stuff []reading
		if req.history {
			stuff = []reading{}
			v.Do(func(x interface{}) {
				if x != nil {
					stuff = append(stuff, x.(reading))
				}
			})
		} else {
			stuff = []reading{v.Value.(reading)}
		}
		response[k] = stuff
	}
	req.ch <- response
}

func newReadingServer() (rv readingServer) {
	rv.current = make(map[string]reading)
	rv.previous = make(map[string]*ring.Ring)
	rv.input = make(chan reading, 1000)
	rv.req = make(chan request)
	return
}

func (rs *readingServer) getReadings() response {
	req := request{
		history: true,
		ch:      make(chan response),
	}
	rs.req <- req
	return <-req.ch
}

func (rs *readingServer) getCurrent(which string) (reading, bool) {
	room, ok := conf.Rooms[which]
	if !ok {
		return reading{}, false
	}
	sn := room.SN

	req := request{
		history: false,
		ch:      make(chan response),
	}
	rs.req <- req
	res := <-req.ch
	if r, ok := res[sn]; ok {
		return r[0], ok
	}
	return reading{}, false
}

func (rs *readingServer) register(ch chan<- reading) {
	rs.registrations = append(rs.registrations, ch)
}
