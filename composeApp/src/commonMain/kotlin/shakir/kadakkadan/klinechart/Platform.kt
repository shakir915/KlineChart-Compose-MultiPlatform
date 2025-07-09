package shakir.kadakkadan.klinechart

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform