package com.thehecklers.sburrestdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SpringBootApplication
public class SburRestDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(SburRestDemoApplication.class, args);
	}

}

class Coffee {
	private final String id;
	private String name;

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

@RestController
@RequestMapping("/coffees")
class RestApiDemoController{
	/*
	* - Coffee 객체의 List 형태로 커피 그룹을 정의
	* - 멤버 변수 타입: 최상위 인터페이스인 제네릭 List로 생성
	* - 비어 있는 ArrayList를 할당해 사용*/
	private List<Coffee> coffees = new ArrayList<>();

	public RestApiDemoController() {
		/*
		* 클래스 생성자: 객체 생성 시 커피 목록을 채우는 코드 추가*/
		coffees.addAll(List.of(
				new Coffee("Cafe Cereza"),
				new Coffee("Cafe Ganador"),
				new Coffee("Cafe Lareno"),
				new Coffee("Cafe Tres Pontas")
		));

	}
	/*
	* - iterable한 커피의 그룹을 멤버 변수인 coffees로 반한화는 메서드 생성
	* - Iterable<Coffee>를 사용하는 이유: 모든 iterable 유형이 이 API의 원하는 기능을 충분히 제공할 것이기 때문*/
	@GetMapping
	Iterable<Coffee> getCoffees() {
		return coffees;
	}

	/*
	* 단일 커피 조회 메서드 추가
	* - 커피 목록에 일치하는 항목 존재 -> 값이 있는 Optional<Coffee> 반환
	* - 요청된 id 값이 없으면 비어 있는 Optional<Coffee>를 반환
	* - 값이 존재하지 않을 수 있는 경우를 안전하게 처리 가능
	* - 코드의 가독성 향상, NullPointerException의 위험 감소
	* */
	@GetMapping("/{id}")
	Optional<Coffee> getCoffeeById(@PathVariable String id) {
		for(Coffee c: coffees) {
			if(c.getId().equals(id)) {
				return Optional.of(c);
			}
		}
		return Optional.empty();
	}

	/*
	* 스프링 부트의 Auto Marshalling
	* - 객체를 직렬화 -> 데이터 형식으로 변환하는 프로세스를 자동으로 처리하는 기능을 의미
	* - 자동 타입 변환 (Java 객체 -> JSON, XML 또는 다른 형식으로 자동 변환하는 기능 제공)
	* - @RestController, @RequestBody 사용시 자동으로 마샬링됨
	* */
	@PostMapping
	Coffee postCoffee(@RequestBody Coffee coffee) {
		coffees.add(coffee);
		return coffee;
	}

	/*
	* PUT 메서드 응답 시 '상태 코드'는 필수
	* - 데이터 저장소에 커피가 존재하지 않는 경우: 201(Created) 반환
	* - 커피가 존재하는 경우: 200(OK) 반환
	* */
	@PutMapping("/{id}")
	ResponseEntity<Coffee> putCoffee(@PathVariable String id, @RequestBody Coffee coffee) {
		/*
		* - coffeeIndex == -1: id와 일치하는 커피가 목록에 없음
		* - coffeeIndex != -1: 해당 id와 일치하는 커피를 목록에서 발견 -> 해당 커피를 업데이트
		* */
		int coffeeIndex = -1;

		for (Coffee c: coffees) {
			if(c.getId().equals(id)) {
				/*
				* 해당 c가 coffees에 존재한다면 -> c의 인덱스 반환
				* 해당 요소가 리스트에 존재하지 않으면 -> -1 반환
				* */
				coffeeIndex = coffees.indexOf(c);
				coffees.set(coffeeIndex, coffee);
			}
		}
		/*
		* - coffeeIndex의 변수가 -1: 일치하는 커피 찾지 못함 -> 새로운 커피 추가 필요(postCoffee())
		* - coffeeIndex의 변수가 -1이 아닌 경우: 업데이트된 coffee 객체를 반환
		* */
		return (coffeeIndex == -1) ?
			new ResponseEntity<>(postCoffee(coffee), HttpStatus.CREATED) :
			new ResponseEntity<>(coffee, HttpStatus.OK);
	}

	@DeleteMapping("/{id}")
	void deleteCoffee(@PathVariable String id) {
		/*
		* Collection의 removeIf()
		* - 인자로 Predicate 필요(특정 조건을 만족하는지 여부를 판단하는 함수를 정의하는데 사용)
		* - true 반환 -> 최소 하나의 요소가 제거됨
		* - false 반환 -> 컬렉션 내의 요소가 변경되지 않음
		* */
		coffees.removeIf(c -> c.getId().equals(id));
	}

}
