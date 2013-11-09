package main

import (
	"encoding/json"
	"fmt"
	"log"
	"reflect"

	couch "github.com/dustin/go-couch"
)

func (r reading) MarshalJSON() ([]byte, error) {
	doc := map[string]interface{}{
		"type":    reflect.TypeOf(r).Name(),
		"reading": r.reading,
		"ts":      r.TS(),
		"sensor":  r.sensor,
	}
	return json.Marshal(doc)
}

func couchStore(msg reading) {
	id := fmt.Sprintf("%s_%s", msg.TS(), msg.sensor)
	db, err := couch.Connect(*couchURL)
	if err != nil {
		log.Printf("Error connecting to DB:  %v", err)
		return
	}
	_, _, ierr := db.InsertWith(msg, id)
	if ierr != nil {
		log.Printf("Error inserting new item:  %v", ierr)
	}
}

func couchLoop() chan<- reading {
	log.Printf("Initializing couch loop to %s", *couchURL)
	ch := make(chan reading, 100)
	go func() {
		for r := range ch {
			couchStore(r)
		}
	}()

	return ch
}
