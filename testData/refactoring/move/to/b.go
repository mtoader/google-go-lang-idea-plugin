package to
import (
    "refactoring/move/from"
    "fmt"
)

func B() {
    fmt.Println("B")
    from.A()
}