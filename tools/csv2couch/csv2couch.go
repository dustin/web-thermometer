package main

import (
	"compress/bzip2"
	"encoding/csv"
	"encoding/json"
	"flag"
	"log"
	"os"
	"sync"
	"time"

	"github.com/dustin/go-couch"
	"github.com/dustin/go-humanize"
)

const ISO8601 = "2006-01-02T15:04:05"

type reading struct {
	ID      string      `json:"_id"`
	Sensor  string      `json:"sensor"`
	Reading json.Number `json:"reading"`
	TS      string      `json:"ts"`
	Type    string      `json:"type"`
}

var (
	db couch.Database
	wg sync.WaitGroup

	ch = make(chan reading)

	maxBatch = flag.Int("maxbatch", 100000,
		"Maximum number of items for a batch")
	flushTimeout = flag.Duration("flushtime", time.Second*15,
		"How frequently to flush")
	dbUrl = flag.String("dburl", "http://localhost:5984/temperature",
		"couchdb url")
)

func maybefatal(err error, msg string, args ...interface{}) {
	if err != nil {
		log.Fatalf(msg, args...)
	}
}

func reformatTs(in string) string {
	t, err := time.Parse(time.RFC3339, in)
	if err != nil {
		panic("Error parsing " + in + " - " + err.Error())
	}
	return t.Format(ISO8601)
}

func enqueue(stuff []string) {
	ts := reformatTs(stuff[2])
	r := reading{
		ID:      ts + "_" + stuff[0],
		Sensor:  stuff[0],
		Reading: json.Number(stuff[1]),
		TS:      ts,
		Type:    "reading",
	}

	ch <- r
}

func flush(a []interface{}) []interface{} {
	log.Printf("Flushing %s items", humanize.Comma(int64(len(a))))
	res, err := db.Bulk(a)
	if err != nil {
		return a
	}
	broken := map[string]bool{}
	for _, r := range res {
		if !r.Ok {
			log.Printf("Error on %v: %v/%v", r.ID, r.Error, r.Reason)
			if r.Error != "conflict" {
				broken[r.ID] = true
			}
		}

	}
	rv := []interface{}{}
	if len(broken) > 0 {
		for _, thing := range a {
			r := thing.(reading)
			if broken[r.ID] {
				rv = append(rv, r)
			}
		}
	}
	return rv
}

func storer() {
	defer wg.Done()
	t := time.After(*flushTimeout)
	var accumulated []interface{}
	for {
		select {
		case r, ok := <-ch:
			if !ok {
				flush(accumulated)
				return
			}
			accumulated = append(accumulated, r)
			if len(accumulated) >= *maxBatch {
				accumulated = flush(accumulated)
			}
			t = time.After(*flushTimeout)
		case <-t:
			accumulated = flush(accumulated)
		}
	}
}

func main() {
	flag.Parse()

	var err error

	db, err = couch.Connect(*dbUrl)
	maybefatal(err, "Error connecting to DB: %v", err)

	f, err := os.Open(flag.Arg(0))
	maybefatal(err, "Error opening %v: %v", flag.Arg(0), err)
	bzr := bzip2.NewReader(f)
	maybefatal(err, "Can't bzip2 -d %v: %v", flag.Arg(0), err)

	cr := csv.NewReader(bzr)

	wg.Add(1)
	go storer()

	report := time.Tick(time.Minute)
	start := time.Now()
	read := 0
	for rec, err := cr.Read(); err == nil; rec, err = cr.Read() {
		enqueue(rec)
		read++

		select {
		case <-report:
			log.Printf("Processed %v in %v",
				humanize.Comma(int64(read)), time.Since(start))
		default:
		}
	}
	close(ch)
	wg.Done()

	log.Printf("Error was %v after %s records in %v", err,
		humanize.Comma(int64(read)), time.Since(start))
}
