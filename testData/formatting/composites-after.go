//gofmt -s

package P

type T struct {
	x, y int
}

var _ = [42]T{
	{},
	{1, 2},
	{3, 4},
}

var _ = [...]T{
	{},
	{1, 2},
	{3, 4},
}

var _ = []T{
	{},
	{1, 2},
	{3, 4},
}

var _ = []T{
	{},
	10: {1, 2},
	20: {3, 4},
}

var _ = []struct {
	x, y int
}{
	{},
	10: {1, 2},
	20: {3, 4},
}

var _ = []interface{}{
	T{},
	10: T{1, 2},
	20: T{3, 4},
}

var _ = [][]int{
	{},
	{1, 2},
	{3, 4},
}

var _ = [][]int{
	([]int{}),
	([]int{1, 2}),
	{3, 4},
}

var _ = [][][]int{
	{},
	{
		{},
		{0, 1, 2, 3},
		{4, 5},
	},
}

var _ = map[string]T{
	"foo": {},
	"bar": {1, 2},
	"bal": {3, 4},
}

var _ = map[string]struct {
	x, y int
}{
	"foo": {},
	"bar": {1, 2},
	"bal": {3, 4},
}

var _ = map[string]interface{}{
	"foo": T{},
	"bar": T{1, 2},
	"bal": T{3, 4},
}

var _ = map[string][]int{
	"foo": {},
	"bar": {1, 2},
	"bal": {3, 4},
}

var _ = map[string][]int{
	"foo": ([]int{}),
	"bar": ([]int{1, 2}),
	"bal": {3, 4},
}

// from exp/4s/data.go
var pieces4 = []Piece{
	{0, 0, Point{4, 1}, []Point{{0, 0}, {1, 0}, {1, 0}, {1, 0}}, nil, nil},
	{1, 0, Point{1, 4}, []Point{{0, 0}, {0, 1}, {0, 1}, {0, 1}}, nil, nil},
	{2, 0, Point{4, 1}, []Point{{0, 0}, {1, 0}, {1, 0}, {1, 0}}, nil, nil},
	{3, 0, Point{1, 4}, []Point{{0, 0}, {0, 1}, {0, 1}, {0, 1}}, nil, nil},
}

var _ = [42]*T{
	{},
	{1, 2},
	{3, 4},
}

var _ = [...]*T{
	{},
	{1, 2},
	{3, 4},
}

var _ = []*T{
	{},
	{1, 2},
	{3, 4},
}

var _ = []*T{
	{},
	10: {1, 2},
	20: {3, 4},
}

var _ = []*struct {
	x, y int
}{
	{},
	10: {1, 2},
	20: {3, 4},
}

var _ = []interface{}{
	&T{},
	10: &T{1, 2},
	20: &T{3, 4},
}

var _ = []*[]int{
	{},
	{1, 2},
	{3, 4},
}

var _ = []*[]int{
	(&[]int{}),
	(&[]int{1, 2}),
	{3, 4},
}

var _ = []*[]*[]int{
	{},
	{
		{},
		{0, 1, 2, 3},
		{4, 5},
	},
}

var _ = map[string]*T{
	"foo": {},
	"bar": {1, 2},
	"bal": {3, 4},
}

var _ = map[string]*struct {
	x, y int
}{
	"foo": {},
	"bar": {1, 2},
	"bal": {3, 4},
}

var _ = map[string]interface{}{
	"foo": &T{},
	"bar": &T{1, 2},
	"bal": &T{3, 4},
}

var _ = map[string]*[]int{
	"foo": {},
	"bar": {1, 2},
	"bal": {3, 4},
}

var _ = map[string]*[]int{
	"foo": (&[]int{}),
	"bar": (&[]int{1, 2}),
	"bal": {3, 4},
}

var pieces4 = []*Piece{
	{0, 0, Point{4, 1}, []Point{{0, 0}, {1, 0}, {1, 0}, {1, 0}}, nil, nil},
	{1, 0, Point{1, 4}, []Point{{0, 0}, {0, 1}, {0, 1}, {0, 1}}, nil, nil},
	{2, 0, Point{4, 1}, []Point{{0, 0}, {1, 0}, {1, 0}, {1, 0}}, nil, nil},
	{3, 0, Point{1, 4}, []Point{{0, 0}, {0, 1}, {0, 1}, {0, 1}}, nil, nil},
}
