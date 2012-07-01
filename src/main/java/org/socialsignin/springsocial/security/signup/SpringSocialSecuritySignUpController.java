/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.socialsignin.springsocial.security.signup;

import java.util.HashMap;
import java.util.Map;

import org.socialsignin.springsocial.security.signin.SpringSocialSecuritySignInService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.web.ProviderSignInUtils;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller for displaying/procesing a sign up form and handling chosen user id, checking
 * if it already exists
 *
* @author Michael Lavelle
*/
@Controller
@RequestMapping("/signup")
public class SpringSocialSecuritySignUpController {

	@Value("${socialsignin.signUpView}")
	private String signUpView;
	
	@Autowired
	private SpringSocialSecuritySignInService springSocialSecuritySignInService;
	
	@Autowired
	private UserDetailsService springSocialSecurityUserDetailsService;
	
	@RequestMapping(value="",method=RequestMethod.GET)
	public String signUpForm(@ModelAttribute("signUpForm") SignUpForm signUpForm)
	{
		return signUpView;
	}
	
	@ModelAttribute("signUpForm")
	public SignUpForm createForm(ServletWebRequest request)
	{
		Connection<?> connection = ProviderSignInUtils.getConnection(request);
		SignUpForm signUpForm = new SignUpForm();
		if (connection != null)
		{
			String thirdPartyUserName = connection.fetchUserProfile().getUsername();
			if (thirdPartyUserName != null && !isUserNameTaken(thirdPartyUserName))
			{
				signUpForm.setUserName(thirdPartyUserName);
			}
		}
		return signUpForm;
	}
	

	@Transactional(readOnly=true)
	private boolean isUserNameTaken(String userName)
	{
		try
		{
			return springSocialSecurityUserDetailsService.loadUserByUsername(userName) != null;
		}
		catch (UsernameNotFoundException e)
		{
			return false;
		}
	}
	
	private boolean isUserNameValid(String userName,BindingResult errors)
	{
		if (userName == null || userName.trim().length() == 0)
		{
			errors.addError(new FieldError("signUpForm","userName","Please choose a username"));
			return false;
		}
		else
		{
			return true;
		}
	}
	
	
	@Transactional(readOnly=false)
	private boolean signUpUser(ServletWebRequest request,String userName,BindingResult errors)
	{
		if (!isUserNameValid(userName,errors))
		{
			return false;
		}
		if (isUserNameTaken(userName))
		{
			errors.addError(new FieldError("signUpForm","userName","Sorry, the username '" + userName + "' is not available"));
			return false;
		}

		ProviderSignInUtils.handlePostSignUp(userName, request);
		return true;
	}
	
	
	@RequestMapping(value="",method=RequestMethod.POST)
	public String signUpSubmit(ServletWebRequest request,@ModelAttribute("signUpForm") SignUpForm signUpForm,BindingResult result)
	{
		Connection<?> connection = ProviderSignInUtils.getConnection(request);	
		signUpUser(request,signUpForm.getUserName(),result);
		if (result.hasErrors())
		{
			return signUpView;
		}
		springSocialSecuritySignInService.signIn(signUpForm.getUserName(), connection, request);
		return "redirect:/authenticate";	

	}
	
}