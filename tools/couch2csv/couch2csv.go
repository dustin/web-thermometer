package main

import (
	"encoding/csv"
	"encoding/json"
	"errors"
	"flag"
	"fmt"
	"io"
	"log"
	"net"
	"net/http"
	"os"
	"strconv"
	"strings"
	"time"

	"github.com/dustin/go-couch"
)

var largest int64

var baseURL string

var httpClient http.Client

var readTimeout = time.Second * 2

var reportInterval = flag.Duration("reportInterval", time.Minute*15,
	"Sequence reporting interval.")
var reportKey = flag.String("reportKey", "_local/couch2csv",
	"Key to store reported sequence in.")

func maybefatal(err error, msg string, args ...interface{}) {
	if err != nil {
		log.Fatalf(msg, args...)
	}
}

type reading struct {
	Reading   float64
	SensorOld *string `json:"sn"`
	SensorNew *string `json:"sensor"`
	Timestamp string  `json:"ts"`
}

type change struct {
	Seq int64
	Id  string
	Doc reading
}

func (r reading) SN() string {
	switch {
	case r.SensorOld != nil:
		return *r.SensorOld
	case r.SensorNew != nil:
		return *r.SensorNew
	}
	return ""
}

func reportSeq(s int64) {
	log.Printf("Recording sequence %v", s)
	db, err := couch.Connect(flag.Arg(0))
	if err != nil {
		log.Printf("Error connecting to couchdb: %v", err)
		return
	}

	m := map[string]interface{}{}
	err = db.Retrieve(*reportKey, &m)
	if err != nil {
		log.Printf("Error pulling report doc: %v", err)
		// Continue with partial data.
	}
	m["_id"] = *reportKey
	m["max_seq"] = s
	m["as_of"] = time.Now()

	if err == nil {
		_, err = db.Edit(m)
	} else {
		_, _, err = db.Insert(m)
	}
	if err != nil {
		log.Printf("Error storing doc:  %v", err)
	}
}

func feedBody(r io.Reader) int64 {
	cw := csv.NewWriter(os.Stdout)
	defer cw.Flush()

	d := json.NewDecoder(r)

	nextReport := time.Now().Add(*reportInterval)

	for {
		thing := change{}
		err := d.Decode(&thing)
		if err != nil {
			if err.Error() == "unexpected EOF" {
				return largest
			} else {
				log.Printf("Error decoding stuff: %#v", err)
				return -1
			}
		}

		ts, err := parseTime(thing.Doc.Timestamp)
		if err != nil {
			continue
		}

		cw.Write([]string{
			thing.Doc.SN(),
			fmt.Sprintf("%f", thing.Doc.Reading),
			ts.Format(time.RFC3339),
		})

		largest = thing.Seq

		now := time.Now()
		if now.After(nextReport) {
			nextReport = now.Add(*reportInterval)
			go reportSeq(thing.Seq)
		}
	}
}

func parseTime(in string) (time.Time, error) {
	parts := strings.FieldsFunc(in, func(r rune) bool {
		return r == '/' || r == '-' || r == ':' || r == 'T' ||
			r == ' ' || r == '.'
	})

	if len(parts) < 6 {
		return time.Time{},
			fmt.Errorf("Incorrect number of fields: %#v", parts)
	}

	np := []int{}
	for _, p := range parts {
		x, err := strconv.Atoi(p)
		if err != nil {
			return time.Time{}, errors.New("Unparsable time")
		}
		np = append(np, x)
	}

	nsec := 0
	if len(np) > 6 {
		nsec = np[6] * 1000
	}

	return time.Date(np[0], time.Month(np[1]), np[2],
		np[3], np[4], np[5], nsec, time.Local), nil
}

func main() {
	since := flag.Int64("since", 0, "Starting seq id")

	flag.Parse()
	db, err := couch.Connect(flag.Arg(0))
	maybefatal(err, "Error connecting: %v", err)

	baseURL = flag.Arg(1)

	var t http.RoundTripper = &http.Transport{
		Proxy:               http.ProxyFromEnvironment,
		MaxIdleConnsPerHost: 50,
		Dial: func(n, addr string) (c net.Conn, err error) {
			c, derr := net.Dial(n, addr)
			return &timeoutConn{c, readTimeout}, derr
		},
	}

	httpClient.Transport = t

	info, err := db.GetInfo()
	maybefatal(err, "Error getting info: %v", err)
	log.Printf("Info %#v", info)

	err = db.Changes(feedBody,
		map[string]interface{}{
			"since":        *since,
			"feed":         "continuous",
			"include_docs": true,
			"timeout":      500,
		})
	if err != nil && err != io.EOF {
		log.Printf("Error changesing: %v", err)
	}
	log.Printf("Largest seen was %v", largest)
}
