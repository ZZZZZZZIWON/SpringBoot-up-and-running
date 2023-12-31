package com.thehecklers.sburrestdemo;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SburRestDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(SburRestDemoApplication.class, args);
	}

	@Bean
	@ConfigurationProperties(prefix = "droid")
	Droid createDroid() {
		return new Droid();
	}
}

@RestController
@RequestMapping("/droid")
class DroidController {
	private final Droid droid;

	public DroidController(Droid droid) {
		this.droid = droid;
	}

	@GetMapping
	Droid getDroid() {
		return droid;
	}

}

class Droid {
	private String id, description;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
/*
*  @Vaule 어노테이션이 멤버 변수 name에 적용되고, 어노테이션 문자열 타입을 단일 매개변수로 하여 속성값을 적음
* */
@RestController
@RequestMapping("/greeting")
class GreetingController {

	private final Greeting greeting;

	GreetingController(Greeting greeting) {
		this.greeting = greeting;
	}

	/*
	* 속성값 SpEL형식
	* - 구분자의 ${ } 사이에 적음
	* - 애플리케이션 환경에서 속성값이 정의되지 않은 경우 -> 기본값을 콜론(:) 뒤에 적기
	* */

	/*
	* @Value의 두 속성은 모두 application.properties에 정의됨 -> 쿼리 결과와 application.properties에 정의한 속성값이 일치
	* */
	@GetMapping
	String getGreeting() {
		return greeting.getName();
	}

	@GetMapping("/coffee")
	String getNameAndCoffee() {
		return greeting.getCoffee();
	}

}
@ConfigurationProperties(prefix = "greeting")
class Greeting {
	private String name;
	private String coffee;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCoffee() {
		return coffee;
	}

	public void setCoffee(String coffee) {
		this.coffee = coffee;
	}
}

@Entity
class Coffee {
	@Id
	private String id;
	private String name;

	/*
	* JPA를 사용해 DB에 데이터 생성시 기본 생성자 필요
	* - 기본 생성자를 사용하기 위해서는 모든 멤버 변수를 final이 아닌 mutable하게 변경해야 함
	* */
	public Coffee() {
	}

	public Coffee(String id, String name) {
		this.id = id;
		this.name = name;
	}

	public Coffee(String name) {
		this(UUID.randomUUID().toString(), name);
	}

	public String getId() {
		return id;
	}


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}

interface CoffeeRepository extends CrudRepository<Coffee, String> {}

/*
* 초기 데이터 생성 기능을 별도의 컴포넌트로 분리 -> 언제든지 쉽게 활성화 or 비활성화 할 수 있도록 refactoring하기
* - @PostConstruct: Spring이 해당 클래스의 빈을 초기화한 후에 자동으로 호출하는 메서드를 정의할 때 사용
* */
@Component
class DataLoader {
	private final CoffeeRepository coffeeRepository;

	public DataLoader(CoffeeRepository coffeeRepository) {
		this.coffeeRepository = coffeeRepository;
	}

	@PostConstruct
	private void loadData() {
		this.coffeeRepository.saveAll(List.of(
				new Coffee("Cafe Cereza"),
				new Coffee("Cafe Ganador"),
				new Coffee("Cafe Lareno"),
				new Coffee("Cafe Tres Pontas")
		));

	}

}

@RestController
@RequestMapping("/coffees")
class RestApiDemoController{

	/*
	* 스프링 4.3 이후 버전 ) 생성자에 단일 파라미터만 있는 경우 -> 해당 파라미터가 자동으로 주입됨
	* */
	/*
	* @Autowired를 사용해야 하는 경우
	* 1. 생성자에 여러 개의 파라미터가 있는 경우
	* 2. 생성자가 없는 경우
	* 3. 특별한 의존성 주입 구성이 필요한 경우
	* */

	private final CoffeeRepository coffeeRepository;

	public RestApiDemoController(CoffeeRepository coffeeRepository) {
		this.coffeeRepository = coffeeRepository;
	}

	@GetMapping
	Iterable<Coffee> getCoffees() {
		/*
		* CrudRepository에 내장된 finaAll() 메서드는 Iteralbe 타입을 반환 -> getCoffee()의 반환 타입 변경 필요 X
		* */
		return coffeeRepository.findAll();
	}

	/*
	* 단일 커피 조회 메서드 추가
	* - findById()는 Optional 타입을 반환
	* */
	@GetMapping("/{id}")
	Optional<Coffee> getCoffeeById(@PathVariable String id) {

		return coffeeRepository.findById(id);
	}

	@PostMapping
	Coffee postCoffee(@RequestBody Coffee coffee) {
		return coffeeRepository.save(coffee);
	}

	@PutMapping("/{id}")
	ResponseEntity<Coffee> putCoffee(@PathVariable String id, @RequestBody Coffee coffee) {

		/*
		* CrudRepository에 내장된 existsById() 사용 가능
		* */
		/*
		* repository를 사용하도록 메서드를 refactoring한 후에는 부정 조건을 먼저 평가할 이유 X -> 코드 가독성 높이기
		* - List 사용: 데이터가 메모리에 저장 -> 명시적으로 부정 연산자를 사용 -> 데이터의 존재 여부를 직접 확인
		* - Repository 사용: 데이터베이스에 데이터를 검색 -> 그 데이터의 존재 여부를 판단 -> 부정 연산자 사용 필요 X
		* */
		return (coffeeRepository.existsById(id)) ?
			new ResponseEntity<>(coffeeRepository.save(coffee), HttpStatus.OK) :
			new ResponseEntity<>(coffeeRepository.save(coffee), HttpStatus.CREATED);
	}

	@DeleteMapping("/{id}")
	void deleteCoffee(@PathVariable String id) {
		coffeeRepository.deleteById(id);
	}

}
