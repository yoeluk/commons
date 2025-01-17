package org.byrde.http.server

//class ServerTest extends AnyFlatSpec with Matchers with ScalaFutures with ScalatestRouteTest with EitherSupport {
//  class TestRoute(
//    override val successCode: Int,
//    fn: () => Future[Either[ErrorResponse, Response.Default]],
//    mapper: EndpointOutput.OneOf[ErrorResponse, ErrorResponse]
//  ) extends ChainingSyntax with MaterializedRoutes with EndpointSupport with CodeSupport {
//    private lazy val test: org.byrde.http.server.MaterializedEndpoint[Unit, ErrorResponse, Response.Default, AkkaStreams with capabilities.WebSockets] =
//      endpoint
//        .in("test")
//        .out(ackOutput)
//        .errorOut(mapper)
//        .route(fn)
//
//    override lazy val routes: Routes =
//      Seq(test)
//  }
//
//  trait TestServer extends HttpServer with ServerTest.ServerTestSupport {
//    override lazy val successCode: Int = 100
//
//    override lazy val config: ServerConfig =
//      new TestAkkaHttpConfig
//
//    override lazy val logger: Logger =
//      new TestLogger
//
//    protected def mapper: EndpointOutput.OneOf[ErrorResponse, ErrorResponse] =
//      errorMapper
//
//    protected lazy val routes: MaterializedRoutes =
//      new TestRoute(successCode, test, mapper)
//
//    protected def test: () => Future[Either[ErrorResponse, Response.Default]] =
//      () =>
//        Future
//          .successful(Right[String, String]("Hello World!"))
//          .toOut {
//            case (_, code) =>
//              Response.Default("Error", code)
//          }
//  }
//
//  "Server.ping" should "return successfully" in new TestServer {
//    HttpRequest(HttpMethods.GET, "/ping") ~> Route.seal(handleMaterializedRoutes(routes)) ~> {
//      check {
//        val entity = responseAs[Json].as[Response.Default].get
//
//        assert(response.header[IdHeader].isDefined)
//        status.intValue shouldBe 200
//        assert(entity.code === successCode)
//      }
//    }
//  }
//
//  "Server.routes" should "return a 200 when function completes successfully" in new TestServer {
//    HttpRequest(HttpMethods.GET, "/test") ~> Route.seal(handleMaterializedRoutes(routes)) ~> {
//      check {
//        val entity = responseAs[Json].as[Response.Default].get
//
//        assert(response.header[IdHeader].isDefined)
//        status.intValue shouldBe 200
//        assert(entity.code === successCode)
//      }
//    }
//  }
//
//  it should "return a 400 when function completes with controlled failure" in new TestServer {
//    HttpRequest(HttpMethods.GET, "/test") ~> Route.seal(handleMaterializedRoutes(routes)) ~> {
//      check {
//        val entity = responseAs[Json].as[Response.Default].get
//
//        assert(response.header[IdHeader].isDefined)
//        status.intValue shouldBe 400
//        assert(entity.code === errorCode)
//      }
//    }
//
//    override lazy val test: () => Future[Either[ErrorResponse, Response.Default]] =
//      () =>
//        Future.successful(Left(Response.Default("Error", errorCode)))
//  }
//
//  it should "return a 404 on bogus path" in new TestServer {
//    HttpRequest(HttpMethods.GET, "/mrgrlgrlgrl") ~> Route.seal(handleMaterializedRoutes(routes)) ~> {
//      check {
//        status.intValue shouldBe 404
//      }
//    }
//
//    override lazy val test: () => Future[Either[ErrorResponse, Response.Default]] =
//      () =>
//        Future.successful(Left(Response.Default("Error", errorCode)))
//  }
//
//  it should "return the status specified by the error mapper" in new HttpServer {
//    self =>
//
//    override lazy val successCode: Int = 100
//
//    override lazy val config: ServerConfig =
//      new TestAkkaHttpConfig
//
//    override lazy val logger: Logger =
//      new TestLogger
//
//    HttpRequest(HttpMethods.GET, "/test") ~> Route.seal(handleMaterializedRoutes(routes)) ~> {
//      check {
//        val entity = responseAs[Json].as[Response.Default].get
//
//        assert(response.header[IdHeader].isDefined)
//        status.intValue shouldBe 403
//        assert(entity.code === errorCode + 2)
//      }
//    }
//
//    class TestRoute extends MaterializedRoutes
//      with EndpointSupport
//      with CodeSupport
//      with ChainingSyntax
//      with ServerTest.ServerTestSupport {
//      case class Test(code: Int, example: String, example1: String) extends Response
//
//      override def successCode: Int = self.successCode
//
//      lazy val mapper: EndpointOutput.OneOf[ErrorResponse, ErrorResponse] =
//        sttp.tapir.oneOf[ErrorResponse](
//          statusMappingValueMatcher(StatusCode.Unauthorized, jsonBody[ErrorResponse].description("Unauthorized!")) {
//            case ex: ErrorResponse if ex.code == errorCode + 1 => true
//          },
//          statusMappingValueMatcher(StatusCode.Forbidden, jsonBody[ErrorResponse].description("Forbidden!")) {
//            case ex: ErrorResponse if ex.code == errorCode + 2 => true
//          }
//        )
//
//      private lazy val test: org.byrde.http.server.MaterializedEndpoint[Unit, ErrorResponse, Test, AkkaStreams with capabilities.WebSockets] =
//        endpoint
//          .in("test")
//          .out(jsonBody[Test])
//          .errorOut(mapper)
//          .route {
//            () =>
//              Future
//                .successful(Left[Int, (String, String)](errorCode + 2))
//                .toOut(
//                  {
//                    case ((example, example1), code) =>
//                      Test(code, example, example1)
//                  }, {
//                    case (code, _) =>
//                      Response.Default("Error", code)
//                  }
//                )
//          }
//
//      override lazy val routes: Routes = test
//    }
//
//    protected lazy val routes: MaterializedRoutes =
//      new TestRoute()
//  }
//
//  it should "return a custom status code based on the error type when function completes with controlled failure" in new TestServer {
//    HttpRequest(HttpMethods.GET, "/test") ~> Route.seal(handleMaterializedRoutes(routes)) ~> {
//      check {
//        val entity = responseAs[Json].as[Response.Default].get
//
//        assert(response.header[IdHeader].isDefined)
//        status.intValue shouldBe 401
//        assert(entity.code === errorCode + 1)
//      }
//    }
//
//    HttpRequest(HttpMethods.GET, "/test") ~> Route.seal(handleMaterializedRoutes(routes)) ~> {
//      check {
//        val entity = responseAs[Json].as[Response.Default].get
//
//        assert(response.header[IdHeader].isDefined)
//        status.intValue shouldBe 403
//        assert(entity.code === errorCode + 2)
//      }
//    }
//
//    private var counter = 0
//
//    override lazy val mapper: EndpointOutput.OneOf[ErrorResponse, ErrorResponse] =
//      sttp.tapir.oneOf[ErrorResponse](
//        statusMappingValueMatcher(StatusCode.Unauthorized, jsonBody[ErrorResponse].description("Client exception!")) {
//          case ex: ErrorResponse if ex.code == errorCode + 1 => true
//        },
//        statusMappingValueMatcher(StatusCode.Forbidden, jsonBody[ErrorResponse].description("Client exception!")) {
//          case ex: ErrorResponse if ex.code == errorCode + 2 => true
//        }
//      )
//
//    override def test: () => Future[Either[ErrorResponse, Response.Default]] =
//      () =>
//        counter
//          .pipe(_ + 1)
//          .tap(counter = _)
//          .pipe(count => Future.successful(Left(Response.Default("Error", errorCode + count))))
//  }
//
//  it should "return a 500 when function completes with unexpected failure" in new TestServer {
//    HttpRequest(HttpMethods.GET, "/test") ~> Route.seal(handleMaterializedRoutes(routes)) ~> {
//      check {
//        val entity = responseAs[Json].as[Response.Default].get
//
//        assert(response.header[IdHeader].isDefined)
//        status.intValue shouldBe 500
//        assert(entity.code === errorCode)
//      }
//    }
//
//    override lazy val test: () => Future[Either[ErrorResponse, Response.Default]] =
//      () => Future.failed(new Exception("Kaboom!"))
//  }
//
//  it should "expose Swagger documentation" in new TestServer {
//    HttpRequest(HttpMethods.GET, "/docs/index.html?url=/docs/docs.yaml") ~> Route.seal(handleMaterializedRoutes(routes)) ~> {
//      check {
//        assert(response.header[IdHeader].isDefined)
//        status.intValue shouldBe 200
//      }
//    }
//  }
//
//  it should "redirect /docs to Swagger documentation" in new TestServer {
//    HttpRequest(HttpMethods.GET, "/docs") ~> Route.seal(handleMaterializedRoutes(routes)) ~> {
//      check {
//        assert(response.header[IdHeader].isDefined)
//        status.intValue shouldBe 308
//      }
//    }
//  }
//
//  it should "expose OpenAPI documentation (alpha)" in new TestServer {
//    private val expected =
//      """openapi: 3.0.3
//        |info:
//        |  title: test
//        |  version: v1.0
//        |paths:
//        |  /test:
//        |    get:
//        |      operationId: getTest
//        |      responses:
//        |        '200':
//        |          description: 'Default response! Success code: 100'
//        |          content:
//        |            application/json:
//        |              schema:
//        |                $ref: '#/components/schemas/Default'
//        |              example:
//        |                message: Success
//        |                code: 100
//        |        '400':
//        |          description: 'Client exception! Error code: 101'
//        |          content:
//        |            application/json:
//        |              schema:
//        |                $ref: '#/components/schemas/Default'
//        |              example:
//        |                message: Error
//        |                code: 101
//        |  /ping:
//        |    get:
//        |      description: Standard API endpoint to say hello to the server.
//        |      operationId: Ping
//        |      responses:
//        |        '200':
//        |          description: 'Default response! Success code: 100'
//        |          content:
//        |            application/json:
//        |              schema:
//        |                $ref: '#/components/schemas/Default'
//        |              example:
//        |                message: Success
//        |                code: 100
//        |        '400':
//        |          description: 'Client exception! Error code: 101'
//        |          content:
//        |            application/json:
//        |              schema:
//        |                $ref: '#/components/schemas/Default'
//        |              example:
//        |                message: Error
//        |                code: 101
//        |components:
//        |  schemas:
//        |    Default:
//        |      required:
//        |      - message
//        |      - code
//        |      type: object
//        |      properties:
//        |        message:
//        |          type: string
//        |        code:
//        |          type: integer
//        |""".stripMargin
//
//    HttpRequest(HttpMethods.GET, "/docs/docs.yaml") ~> Route.seal(handleMaterializedRoutes(routes)) ~> {
//      check {
//        val actual = response.entity.toStrict(1.seconds).map(_.data.utf8String).futureValue
//
//        status.intValue shouldBe 200
//        assert(actual === expected)
//      }
//    }
//  }
//
//  it should "expose OpenAPI documentation (beta)" in new HttpServer {
//    self =>
//
//    override lazy val successCode: Int = 100
//
//    override lazy val config: ServerConfig =
//      new TestAkkaHttpConfig
//
//    override lazy val logger: Logger =
//      new TestLogger
//
//    private val expected =
//      """openapi: 3.0.3
//        |info:
//        |  title: test
//        |  version: v1.0
//        |paths:
//        |  /test:
//        |    get:
//        |      operationId: getTest
//        |      responses:
//        |        '200':
//        |          description: ''
//        |          content:
//        |            application/json:
//        |              schema:
//        |                $ref: '#/components/schemas/Test'
//        |        '401':
//        |          description: Unauthorized!
//        |          content:
//        |            application/json:
//        |              schema:
//        |                $ref: '#/components/schemas/Default'
//        |        '403':
//        |          description: Forbidden!
//        |          content:
//        |            application/json:
//        |              schema:
//        |                $ref: '#/components/schemas/Default'
//        |  /ping:
//        |    get:
//        |      description: Standard API endpoint to say hello to the server.
//        |      operationId: Ping
//        |      responses:
//        |        '200':
//        |          description: 'Default response! Success code: 100'
//        |          content:
//        |            application/json:
//        |              schema:
//        |                $ref: '#/components/schemas/Default'
//        |              example:
//        |                message: Success
//        |                code: 100
//        |        '400':
//        |          description: 'Client exception! Error code: 101'
//        |          content:
//        |            application/json:
//        |              schema:
//        |                $ref: '#/components/schemas/Default'
//        |              example:
//        |                message: Error
//        |                code: 101
//        |components:
//        |  schemas:
//        |    Default:
//        |      required:
//        |      - message
//        |      - code
//        |      type: object
//        |      properties:
//        |        message:
//        |          type: string
//        |        code:
//        |          type: integer
//        |    Test:
//        |      required:
//        |      - code
//        |      - example
//        |      - example1
//        |      type: object
//        |      properties:
//        |        code:
//        |          type: integer
//        |        example:
//        |          type: string
//        |        example1:
//        |          type: string
//        |""".stripMargin
//
//    HttpRequest(HttpMethods.GET, "/docs/docs.yaml") ~> Route.seal(handleMaterializedRoutes(routes)) ~> {
//      check {
//        val actual = response.entity.toStrict(1.seconds).map(_.data.utf8String).futureValue
//
//        status.intValue shouldBe 200
//        assert(actual === expected)
//      }
//    }
//
//    class TestRoute
//      extends Materia
//        with CodeSupport
//        with ChainingSyntax
//        with ServerTest.ServerTestSupport {
//      case class Test(code: Int, example: String, example1: String) extends Response
//
//      override def successCode: Int = self.successCode
//
//      private lazy val mapper: EndpointOutput.OneOf[ErrorResponse, ErrorResponse] =
//        sttp.tapir.oneOf[ErrorResponse](
//          statusMappingValueMatcher(StatusCode.Unauthorized, jsonBody[ErrorResponse].description("Unauthorized!")) {
//            case ex: ErrorResponse if ex.code == errorCode + 1 => true
//          },
//          statusMappingValueMatcher(StatusCode.Forbidden, jsonBody[ErrorResponse].description("Forbidden!")) {
//            case ex: ErrorResponse if ex.code == errorCode + 2 => true
//          }
//        )
//
//      private lazy val test: org.byrde.http.server.MaterializedEndpoint[Unit, ErrorResponse, Test, AkkaStreams with capabilities.WebSockets] =
//        endpoint
//          .in("test")
//          .out(jsonBody[Test])
//          .errorOut(mapper)
//          .route {
//            () =>
//              Future
//                .successful(Right(("Hello World!", "Goodbye World!")))
//                .toOut {
//                  case ((example, example1), code) =>
//                    Test(code, example, example1)
//                }
//          }
//
//      override lazy val routes: Routes = test
//    }
//
//    private lazy val routes =
//      new TestRoute
//  }
//}
//
//object ServerTest {
//  trait ServerTestSupport {
//    self: CodeSupport =>
//
//    implicit class RichResponse[T, TT](future: Future[Either[TT, T]]) {
//      def toOut[A <: Response](
//        success: (T, Int) => A,
//        error: (TT, Int) => ErrorResponse =
//        (_, code) => Response.Default("Error", code)
//      )(implicit ec: ExecutionContext): Future[Either[ErrorResponse, A]] =
//        future.map {
//          case Right(succ) =>
//            Right(success(succ, self.successCode))
//
//          case Left(err) =>
//            Left(error(err, self.errorCode))
//        }
//    }
//  }
//}
