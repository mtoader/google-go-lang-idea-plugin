package main

var <warning descr="Unused variable 't'">t</warning> struct {
  *int
  string
  <error descr="Invalid type in pointer">*[]int32</error>
  <error descr="Invalid type in pointer">*(uint)</error>
  <error descr="Invalid type in pointer">**float32</error>
  <error descr="Invalid type in pointer">*struct{}</error>
}