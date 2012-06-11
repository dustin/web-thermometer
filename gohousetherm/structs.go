package main

import (
	"container/ring"
	"time"
)

type response map[string][]reading

type request struct {
	history bool
	ch      chan response
}

type readingServer struct {
	current  map[string]reading
	previous map[string]*ring.Ring

	registrations []chan<- reading

	input      chan reading
	req        chan request
	registerch chan chan<- reading
}

type reading struct {
	when    time.Time
	sensor  string
	reading float64
}

func (r reading) TS() string {
	return r.when.Format(timeOutFormat)
}
