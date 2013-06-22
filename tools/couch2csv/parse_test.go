package main

import (
	"testing"
	"time"
)

func TestTimeParsing(t *testing.T) {
	inputs := []struct {
		input string
		exp   string
	}{
		{"2011-10-15T10:47:58", "2011-10-15T17:47:58Z"},
		{"2005/07/09 19:26:24", "2005-07-10T02:26:24Z"},
		{"2011/06/15 23:42:23.583795", "2011-06-16T06:42:23.583795Z"},
	}

	for _, i := range inputs {
		tm, err := parseTime(i.input)
		if err != nil {
			t.Errorf("Error parsing %v: %v", i.input, err)
			t.Fail()
		} else {
			got := tm.UTC().Format(time.RFC3339Nano)
			if got != i.exp {
				t.Errorf("Expected %v for %v, got %v",
					i.exp, i.input, got)
				t.Fail()
			}
		}
	}
}
