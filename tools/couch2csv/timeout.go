package main

import (
	"net"
	"time"
)

type timeoutConn struct {
	socket      net.Conn
	readTimeout time.Duration
}

func (tc *timeoutConn) Read(b []byte) (n int, err error) {
	tc.SetReadDeadline(time.Now().Add(tc.readTimeout))
	return tc.socket.Read(b)
}

func (tc *timeoutConn) Write(b []byte) (n int, err error) {
	tc.SetReadDeadline(time.Now().Add(tc.readTimeout))
	tc.SetWriteDeadline(time.Now().Add(tc.readTimeout))
	defer tc.SetWriteDeadline(time.Time{})
	return tc.socket.Write(b)
}

func (tc *timeoutConn) Close() error {
	tc.SetDeadline(time.Now().Add(tc.readTimeout))
	return tc.socket.Close()
}

func (tc *timeoutConn) LocalAddr() net.Addr {
	return tc.socket.LocalAddr()
}

func (tc *timeoutConn) RemoteAddr() net.Addr {
	return tc.socket.RemoteAddr()
}

func (tc *timeoutConn) SetDeadline(t time.Time) error {
	return tc.socket.SetDeadline(t)
}

func (tc *timeoutConn) SetReadDeadline(t time.Time) error {
	return tc.socket.SetReadDeadline(t)
}

func (tc *timeoutConn) SetWriteDeadline(t time.Time) error {
	return tc.socket.SetWriteDeadline(t)
}
