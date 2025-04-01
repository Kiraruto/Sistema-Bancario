package com.github.kiraruto.sistemaBancario;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Application {

//	@Autowired
//	private UserRepository userRepository;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

//	@Override
//	public void run(String... args) throws Exception {
//		List<User> adminAccount = userRepository.findByRole(EnumUserRole.ADMIN);
//		if (adminAccount.isEmpty()) {
//			User user = new User();
//
//            user.setUsername("admin");
//			user.setEmail("admin@gmail.com");
//			user.setPassword("admin");
//			user.setRole(EnumUserRole.ADMIN);
//			user.setActive(true);
//
//			userRepository.save(user);
//		}
//	}


}
