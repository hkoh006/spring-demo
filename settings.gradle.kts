rootProject.name = "spring-demo"

include("crypto-exchange-server")
include("crypto-exchange-server-docker:local")
include("crypto-exchange-server-docker:server")

include("order-generator")

include("web-service")
include("web-service-docker:local")
include("web-service-docker:server")
