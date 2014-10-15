package main

import "fmt"
func makeMap()(map[string]interface{}) {
    return map[string]interface{}{"foo":1}
}
func main() {
    a:=func(i int) (theMap map[string]interface{}) {
    theMap = makeMap()
    theMap["ai"]=1
        return
    }

    print(a(2))
}