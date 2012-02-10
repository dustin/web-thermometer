package main

import (
	"container/ring"
	"log"
	"net"
	"strconv"
	"strings"
	"time"
)

var timeInFormat string = "2006/01/02 15:04:05"
var timeOutFormat string = "2006-01-02T15:04:05"

var readingsSingleton readings

func processReadings() {
	for {
		select {
		case r := <-readingsSingleton.input:
			readingsSingleton.current[r.sensor] = r
			rng := readingsSingleton.previous[r.sensor]
			if rng == nil {
				rng = ring.New(100)
			}
			rng = rng.Prev()
			rng.Value = r
			readingsSingleton.previous[r.sensor] = rng
		case r := <-readingsSingleton.req:
			response := make(map[string][]reading)
			for k, v := range readingsSingleton.previous {
				stuff := []reading{}
				v.Do(func(x interface{}) {
					if x != nil {
						stuff = append(stuff, x.(reading))
					}
				})
				response[k] = stuff
			}
			r <- response
		}
	}
}

func read(s *net.UDPConn, ch chan reading) {
	b := make([]byte, 256)
	for {
		n, e := s.Read(b)
		if e != nil {
			log.Fatalf("Error reading from socket:  %s", e)
		}
		parts := strings.Split(string(b[0:n]), "\t")
		f, ferr := strconv.ParseFloat(parts[2], 64)
		if ferr != nil {
			log.Printf("Error parsing float:  %s", ferr)
			continue
		}
		t, terr := time.Parse(timeInFormat, strings.Split(parts[0], ".")[0])
		if terr != nil {
			log.Printf("Error parsing time: %s", terr)
			continue
		}

		// Do this to convert to UTC
		// t = time.SecondsToUTC(t.Seconds() - int64(time.LocalTime().ZoneOffset))

		record := reading{
			when:    t,
			sensor:  parts[1],
			reading: f,
		}
		ch <- record
	}
}

func readNet() error {
	socket, err := net.ListenMulticastUDP("udp4",
		nil, &net.UDPAddr{
			IP:   net.IPv4(225, 0, 0, 37),
			Port: 6789,
		})

	readingsSingleton = newReadings()
	if err != nil {
		return err
	}

	go processReadings()

	go read(socket, readingsSingleton.input)
	return nil
}
