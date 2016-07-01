package main

import (
	"os"
	"fmt"
	"io"
)

func main() {
	f, _ := os.Open("re.go")
	NewReader(f)
}

func NewReader(rd io.Reader) {
	fmt.Println(rd)
}