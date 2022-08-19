package com.odebar.junit.service;

import com.odebar.junit.dao.UserDao;
import com.odebar.junit.dto.User;
import com.odebar.junit.extension.*;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsMapContaining;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("fast")
@Tag("user")
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.DisplayName.class)
@ExtendWith({
        UserServiceParamResolver.class,
        GlobalExtension.class,
        PostProcessingExtension.class,
        ConditionalExtension.class,
        MockitoExtension.class
        //ThrowableExtension.class
})
class UserServiceTest {

    private static final User IVAN = User.of(1, "Ivan", "123");
    private static final User PETR = User.of(2, "Petr", "111");

//    @Rule
//    ExpectedException

    @Captor
    private ArgumentCaptor<Integer> argumentCaptor;
    @Mock(lenient = true)
    private UserDao userDao;
    @InjectMocks
    private UserService userService;

    UserServiceTest(TestInfo testInfo) {
        System.out.println();
    }

    @BeforeAll
    void init() {
        System.out.println("Before all: " + this);
    }

    @BeforeEach
    void prepare() {
        System.out.println("Before each: " + this);
        Mockito.doReturn(true).when(userDao).delete(IVAN.getId());
        //        Mock
//        this.userDao = Mockito.mock(UserDao.class);
//        this.userService = new UserService(userDao);

//      Spy
//        System.out.println("Before each: " + this);
//        this.userDao = Mockito.spy(new UserDao());
//        this.userService = new UserService(userDao);
    }

    @Test
    void throwExceptionIfDatabaseIsNotAvailable() {
        Mockito.doThrow(RuntimeException.class).when(userDao).delete(IVAN.getId());

        assertThrows(RuntimeException.class, () -> userService.delete(IVAN.getId()));
    }

    @Test
    void shouldDeleteExistedUser() {
        userService.add(IVAN);
        Mockito.doReturn(true).when(userDao).delete(IVAN.getId());
//        Mockito.doReturn(true).when(userDao).delete(Mockito.any());

//        Mockito.when(userDao.delete(IVAN.getId()))
//                .thenReturn(true)
//                .thenReturn(false);

        boolean deleteResult = userService.delete(IVAN.getId());
        System.out.println(userService.delete(IVAN.getId()));
        System.out.println(userService.delete(IVAN.getId()));

        Mockito.verify(userDao, Mockito.times(3)).delete(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue()).isEqualTo(IVAN.getId());


        assertThat(deleteResult).isTrue();
    }

    @Test
    @Order(1)
    @DisplayName("users will be empty if no user added")
    void usersEmptyIfNoUserAdded(UserService userService) throws IOException {
        if (true) {
            throw new RuntimeException();
        }
        System.out.println("Test 1: " + this);
        var users = userService.getAll();

        MatcherAssert.assertThat(users, empty());
        assertTrue(users.isEmpty(), "User list should be empty");
//        input -> [box == func] -> actual output
    }

    @Test
    @Order(2)
    void usersSizeIfUserAdded() {
        System.out.println("Test 2: " + this);
        userService.add(IVAN);
        userService.add(PETR);

        var users = userService.getAll();

        assertThat(users).hasSize(2);
//        assertEquals(2, users.size());
    }

    @Test
    void usersConvertedToMapById() {
        userService.add(IVAN, PETR);

        Map<Integer, User> users = userService.getAllConvertedById();

        MatcherAssert.assertThat(users, IsMapContaining.hasKey(IVAN.getId()));
        assertAll(
                () -> assertThat(users).containsKeys(IVAN.getId(), PETR.getId()),
                () -> assertThat(users).containsValues(IVAN, PETR)
        );
    }

    @AfterEach
    void deleteDataFromDatabase() {
        System.out.println("After each: " + this);
    }

    @AfterAll
    void closeConnectionPool() {
        System.out.println("After all: " + this);
    }

    @Nested
    @DisplayName("test user login functionality")
    @Tag("login")
    class LoginTest {

        @Test
        @Disabled("flaky, need to see")
        void loginFailIfPasswordIsNotCorrect() {
            userService.add(IVAN);

            var maybeUser = userService.login(IVAN.getUsername(), "dummy");

            assertTrue(maybeUser.isEmpty());
        }

        @Test
        void loginFailIfUserDoesNotExist() {
            userService.add(IVAN);

            var maybeUser = userService.login("dummy", IVAN.getPassword());

            assertTrue(maybeUser.isEmpty());
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

//    @org.junit.Test(expected = IllegalArgumentException.class)
        void throwExceptionIfUsernameOrPasswordIsNull() {
            assertAll(
                    () -> {
                        var exception = assertThrows(IllegalArgumentException.class, () -> userService.login(null, "dummy"));
                        assertThat(exception.getMessage()).isEqualTo("username or password is null");
                    },
                    () -> assertThrows(IllegalArgumentException.class, () -> userService.login("dummy", null))
            );
        }

        @ParameterizedTest
        //@ArgumentsSource()
//        @NullSource
//        @EmptySource
        //@NullAndEmptySource
        //@ValueSource
        @MethodSource("com.odebar.junit.service.UserServiceTest#getArgumentForLoginTest")
        void loginParametrizedTest(String username, String password, Optional<User> user) {
            userService.add(IVAN, PETR);

            Optional<User> maybeUser = userService.login(username, password);
            assertThat(maybeUser).isEqualTo(user);
        }
    }

    static Stream<Arguments> getArgumentForLoginTest() {
        return Stream.of(
                Arguments.of("Ivan", "123", Optional.of(IVAN)),
                Arguments.of("Petr", "111", Optional.of(PETR))
//                Arguments.of("Petr", "dummy", Optional.of(IVAN)),
//                Arguments.of("dummy", "123", Optional.of(IVAN))
        );
    }
}