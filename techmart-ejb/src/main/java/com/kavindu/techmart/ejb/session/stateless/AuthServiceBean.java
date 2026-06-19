package com.kavindu.techmart.ejb.session.stateless;

import com.kavindu.techmart.common.dto.UserDTO;
import com.kavindu.techmart.common.entity.User;
import com.kavindu.techmart.common.entity.UserSession;
import com.kavindu.techmart.common.enums.UserRole;
import com.kavindu.techmart.common.exception.AuthException;
import com.kavindu.techmart.common.exception.ResourceNotFoundException;
import com.kavindu.techmart.common.interfaces.AuthServiceLocal;
import com.kavindu.techmart.ejb.session.singleton.PerformanceMetricsBean;
import com.kavindu.techmart.ejb.util.Mappers;
import com.kavindu.techmart.ejb.util.PasswordUtil;
import jakarta.ejb.EJB;
import jakarta.ejb.Local;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDateTime;
import java.util.logging.Logger;

@Stateless
@Local(AuthServiceLocal.class)
public class AuthServiceBean implements AuthServiceLocal {

    private static final Logger LOG = Logger.getLogger(AuthServiceBean.class.getName());
    private static final int SESSION_HOURS = 8;

    @PersistenceContext(unitName = "TechMartPU")
    private EntityManager em;

    @EJB
    private PerformanceMetricsBean metrics;

    @Override
    public UserDTO login(String username, String password) {
        if (username == null || password == null) {
            throw new AuthException("Username and password are required");
        }
        User user;
        try {
            user = em.createNamedQuery("User.findByUsername", User.class)
                    .setParameter("username", username.trim())
                    .getSingleResult();
        } catch (NoResultException e) {
            throw new AuthException("Invalid username or password");
        }
        if (!user.isActive()) {
            throw new AuthException("Account is disabled");
        }
        if (!PasswordUtil.verify(password, user.getSalt(), user.getPasswordHash())) {
            throw new AuthException("Invalid username or password");
        }

        UserSession session = new UserSession();
        session.setUser(user);
        session.setToken(PasswordUtil.generateToken());
        session.setExpiresAt(LocalDateTime.now().plusHours(SESSION_HOURS));
        session.setActive(true);
        em.persist(session);

        metrics.incrementActiveUsers();
        LOG.info("Login success for user '" + username + "' (role=" + user.getRole() + ")");

        UserDTO dto = Mappers.toUserDTO(user);
        dto.setToken(session.getToken());
        return dto;
    }

    @Override
    public void logout(String token) {
        if (token == null) {
            return;
        }
        try {
            UserSession session = em.createNamedQuery("UserSession.findByToken", UserSession.class)
                    .setParameter("token", token)
                    .getSingleResult();
            session.setActive(false);
            metrics.decrementActiveUsers();
            LOG.info("Logout for user id " + session.getUser().getId());
        } catch (NoResultException ignored) {

        }
    }

    @Override
    public UserDTO register(UserDTO dto, String rawPassword) {
        if (dto == null || dto.getUsername() == null || dto.getEmail() == null || rawPassword == null) {
            throw new AuthException("Username, email and password are required");
        }
        if (rawPassword.length() < 6) {
            throw new AuthException("Password must be at least 6 characters");
        }
        if (countByUsername(dto.getUsername().trim()) > 0) {
            throw new AuthException("Username is already taken");
        }
        if (countByEmail(dto.getEmail().trim()) > 0) {
            throw new AuthException("Email is already registered");
        }

        User user = new User();
        user.setUsername(dto.getUsername().trim());
        user.setEmail(dto.getEmail().trim());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPhone(dto.getPhone());

        user.setRole(UserRole.CUSTOMER);
        user.setActive(true);
        String salt = PasswordUtil.generateSalt();
        user.setSalt(salt);
        user.setPasswordHash(PasswordUtil.hash(rawPassword, salt));
        em.persist(user);
        LOG.info("Registered new customer '" + user.getUsername() + "'");
        return Mappers.toUserDTO(user);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public UserDTO validateToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            UserSession session = em.createNamedQuery("UserSession.findByToken", UserSession.class)
                    .setParameter("token", token)
                    .getSingleResult();
            if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
                return null;
            }
            return Mappers.toUserDTO(session.getUser());
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public UserDTO getUserById(Long id) {
        User user = em.find(User.class, id);
        if (user == null) {
            throw new ResourceNotFoundException("User not found: " + id);
        }
        return Mappers.toUserDTO(user);
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = em.find(User.class, userId);
        if (user == null) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }
        if (!PasswordUtil.verify(oldPassword, user.getSalt(), user.getPasswordHash())) {
            throw new AuthException("Current password is incorrect");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new AuthException("New password must be at least 6 characters");
        }
        String salt = PasswordUtil.generateSalt();
        user.setSalt(salt);
        user.setPasswordHash(PasswordUtil.hash(newPassword, salt));
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public long getActiveSessionCount() {
        return em.createNamedQuery("UserSession.countActive", Long.class)
                .setParameter("now", LocalDateTime.now())
                .getSingleResult();
    }

    private long countByUsername(String username) {
        return em.createQuery("SELECT COUNT(u) FROM User u WHERE u.username = :u", Long.class)
                .setParameter("u", username)
                .getSingleResult();
    }

    private long countByEmail(String email) {
        return em.createQuery("SELECT COUNT(u) FROM User u WHERE u.email = :e", Long.class)
                .setParameter("e", email)
                .getSingleResult();
    }
}
