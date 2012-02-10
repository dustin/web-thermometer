package main

import (
	"log"
	"net"
	"strconv"
	"strings"
	"time"
)

type reading struct {
	when    time.Time
	sensor  string
	reading float64
}

var timeInFormat string = "2006/01/02 15:04:05"
var timeOutFormat string = "2006-01-02T15:04:05"

func (r reading) TS() string {
	return r.when.Format(timeOutFormat)
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

func readNet() (<-chan reading, error) {
	socket, err := net.ListenMulticastUDP("udp4",
		nil, &net.UDPAddr{
			IP:   net.IPv4(225, 0, 0, 37),
			Port: 6789,
		})

	ch := make(chan reading, 1000)
	if err != nil {
		return ch, err
	}

	go read(socket, ch)
	return ch, nil
}
