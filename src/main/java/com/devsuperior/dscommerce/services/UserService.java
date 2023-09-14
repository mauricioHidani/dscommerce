package com.devsuperior.dscommerce.services;

import com.devsuperior.dscommerce.dto.UserDTO;
import com.devsuperior.dscommerce.entities.Role;
import com.devsuperior.dscommerce.entities.User;
import com.devsuperior.dscommerce.projections.UserDetailsProjection;
import com.devsuperior.dscommerce.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService implements UserDetailsService {

	@Autowired
	private UserRepository repository;
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		
		List<UserDetailsProjection> result = repository.searchUserAndRolesByEmail(username);
		if (result.size() == 0) {
			throw new UsernameNotFoundException("Email not found");
		}
		
		User user = new User();
		user.setEmail(result.get(0).getUsername());
		user.setPassword(result.get(0).getPassword());
		result.stream()
			  .forEach(projection ->
					  user.addAuthority(new Role(projection.getRoleId(),
							  					 projection.getAuthority()
					  ))
			  );
		
		return user;
	}

	protected User authenticated() {
		try {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication(); // captura usuário autenticado que está no contexto atual da app
			Jwt jwtPrincipal = (Jwt) auth.getPrincipal(); // caprura o jwt desse contexto atual
			String username = jwtPrincipal.getClaim("username"); // captura o claim do jwt que está denominado como `username`
			return repository.findByEmail(username).get(); // usa do repository para recuperar o usuário com o `username` do contexto atual
		}
		catch (Exception e) {
			throw new UsernameNotFoundException("Email not found!");
		}
	}

	@Transactional(readOnly = true)
	public UserDTO getMe() {
		User currAuthenticated = authenticated();
		return new UserDTO(currAuthenticated);
	}

}
