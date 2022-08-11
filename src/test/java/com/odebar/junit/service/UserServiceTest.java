package com.odebar.junit.service;

import com.odebar.junit.dto.User;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.collection.IsMapContaining;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class UserServiceTest {

    private static final User IVAN = User.of(1, "Ivan", "123");
    private static final User PETER = User.of(2, "Peter", "111");
    private UserService userService;

    @BeforeAll
    static void init() {
        System.out.println("BeforeAll : ");
    }

    @BeforeEach
    void prepare() {
        System.out.println("BeforeEach : " + this);
        userService = new UserService();
    }

    @Test
    void usersEmptyIfNoUsersAdded() {
        System.out.println("Test1 : " + this);
        List<User> users = userService.getAll();

        MatcherAssert.assertThat(users, IsEmptyCollection.empty());
        assertTrue(users.isEmpty(), () -> "Users List should be empty");
    }

    @Test
    void usersSizeIfUsersAdded() {
        System.out.println("Test2 : " + this);
        userService.add(IVAN);
        userService.add(PETER);

        List<User> users = userService.getAll();

        assertThat(users).hasSize(2);
        // assertEquals(2, users.size());
    }

    @Test
    void loginSuccessIfUserExists() {
        userService.add(IVAN);
        Optional<User> maybeUser = userService.login(IVAN.getUsername(), IVAN.getPassword());

        assertThat(maybeUser).isPresent();
        maybeUser.ifPresent(user -> assertThat(user).isEqualTo(IVAN));

//        assertTrue(maybeUser.isPresent());
//        maybeUser.ifPresent(user -> assertEquals(IVAN, user));
    }

    @Test
    void usersConvertedToMapById() {
        userService.add(IVAN, PETER);

        Map<Integer, User> users = userService.getAllConvertedById();

        MatcherAssert.assertThat(users, IsMapContaining.hasKey(IVAN.getId()));

        assertAll(
                () -> assertThat(users).containsKeys(IVAN.getId(), PETER.getId()),
                () -> assertThat(users).containsValues(IVAN, PETER)
        );

    }

    @Test
    void loginFailedIfPasswordIsNotCorrect() {
        userService.add(IVAN);
        Optional<User> maybeUser = userService.login(IVAN.getUsername(), "dummy");

        assertTrue(maybeUser.isEmpty());
    }

    @Test
    void loginFailedIfUserDoesNotExists() {
        userService.add(IVAN);
        Optional<User> maybeUser = userService.login("dummy", IVAN.getPassword());

        assertTrue(maybeUser.isEmpty());
    }

    @AfterEach
    void deleteFromDatabase() {
        System.out.println("AfterEach : " + this);
    }

    @AfterAll
    static void closeConnectionPool() {
        System.out.println("AfterAll : ");
    }
}
