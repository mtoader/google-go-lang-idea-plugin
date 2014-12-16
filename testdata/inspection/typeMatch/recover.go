package typeMatch

func good1() {
	defer func() {
		if err := recover(); err != nil {
		}
	}()
}