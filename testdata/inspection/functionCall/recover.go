package functionCall

func f1(a,b interface {}){

}

func f2() {
	defer func() {
		err := recover()
		f1(1,err)
	}()
}