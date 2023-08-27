package dog.shebang
package utility.typeconstructor

import cats.data.State

type CurriedState[S] = [A] =>> State[S, A] 
