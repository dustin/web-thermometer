package main

import (
	"image/color"
	"math"
)

func HSL2RGB(h, sl, l float64) (rv color.NRGBA) {
	var v, r, g, b float64

	r = l // default to gray
	g = l
	b = l
	if l <= 0.5 {
		v = (l * (1.0 + sl))
	} else {
		v = (l + sl - l*sl)
	}
	if v > 0 {
		var m, sv float64
		var sextant int
		var fract, vsf, mid1, mid2 float64

		m = l + l - v
		sv = (v - m) / v
		h *= 6.0
		sextant = int(h)
		fract = h - float64(sextant)
		vsf = v * sv * fract
		mid1 = m + vsf
		mid2 = v - vsf
		switch sextant {
		case 0:
			r = v
			g = mid1
			b = m
			break
		case 1:
			r = mid2
			g = v
			b = m
			break
		case 2:
			r = m
			g = v
			b = mid1
			break
		case 3:
			r = m
			g = mid2
			b = v
			break
		case 4:
			r = mid1
			g = m
			b = v
			break
		case 5:
			r = v
			g = m
			b = mid2
			break
		}
	}
	rv.R = uint8(r * 255.0)
	rv.G = uint8(g * 255.0)
	rv.B = uint8(b * 255.0)
	rv.A = 255
	return
}

func RGB2HSL(rgb color.Color) (h, s, l float64) {
	r, g, b, _ := rgb.RGBA()
	r /= 255.0
	g /= 255.0
	b /= 255.0

	var v, m, vm, r2, g2, b2 float64

	v = math.Max(float64(r), float64(g))
	v = math.Max(v, float64(b))
	m = math.Min(float64(r), float64(g))
	m = math.Min(m, float64(b))

	l = (m + v) / 2.0
	if l <= 0.0 {
		return
	}
	vm = v - m
	s = vm
	if s > 0.0 {
		if l <= 0.5 {
			s /= (v + m)
		} else {
			s /= (2.0 - v - m)
		}
	} else {
		return
	}
	r2 = (v - float64(r)) / vm
	g2 = (v - float64(g)) / vm
	b2 = (v - float64(b)) / vm
	if float64(r) == v {
		if float64(g) == m {
			h = 5.0 + b2
		} else {
			h = 1.0 - g2
		}
	} else if float64(g) == v {
		if float64(b) == m {
			h = 1.0 + r2
		} else {
			h = 3.0 - b2
		}
	} else {
		if float64(r) == m {
			h = 3.0 + g2
		} else {
			h = 5.0 - r2
		}
	}
	h /= 6.0
	return
}
