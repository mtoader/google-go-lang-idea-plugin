package demo2031

import (
	"time"
)

type <weak_warning descr="'Duration' should have a comment or not be exported">Duration</weak_warning> time.Duration

func (_ *<error descr="Unresolved type 'string'">string</error>) Demo() {

}

func (_ <error descr="Unresolved type 'int'">int</error>) Demo() {

}

func (_ <error descr="Unresolved type 'ScanState'">ScanState</error>) Demo() {

}

func (_ *Duration) <weak_warning descr="'UnmarshalText' should have a comment or not be exported">UnmarshalText</weak_warning>(data []byte) (err error) {
	_ = data
	return nil
}