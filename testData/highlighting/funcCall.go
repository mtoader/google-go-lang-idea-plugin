package mongo

type Collection struct{}
type CollectionLoaderInterface interface {
    MangoCollection(string) *Collection
}

func <warning descr="Unused function 'Collection'">Collection</warning>(parent interface{},collection string) *Collection{
     switch parent := parent.(type) {
          case CollectionLoaderInterface:
          return parent.MangoCollection(collection) // Inspection error: too many arguments...
     }
     return nil
}