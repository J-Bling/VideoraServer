package com.server.dao.user;

import com.server.dto.response.user.UserResponse;
import com.server.entity.user.User;
import com.server.message.service.impl.ChatServiceImpl;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface UserDao {
    @Select("select admin from user where id=#{id}")
    boolean isAdminById(@Param("id") int id);
    @Select("select lock from user where id=#{id}")
    boolean isLockById(@Param("id") int id);
    @Delete("delete from user where id=#{id}")
    int deleteUserById(@Param("id") int id);

    User findUserByPhone(@Param("phone") String phone);
    User findUserByEmail(@Param("email") String email);

    @Select("select id from user where email=#{email}")
    Integer findUserIdByEmail(@Param("email") String email);

    @Select("select id from user where phone=#{phone}")
    Integer findUserIdByPhone(@Param("phone") String phone);

    @Select("select password,salt from user where id =#{userId}")
    User findUserPassword(@Param("userId") int userId);

    User findUserById(@Param("id") Integer id);


    int increaseUserByPhone(User user);
    int increaseUserByEmail(User user);

    /**
     * just find : id,nickname,gender,avatar_url,description
     * @param id userId
     * @return id,nickname,gender,avatar_url,description;
     */
    UserResponse findUserDataById(@Param("id") Integer id);

    List<UserResponse> findFriends(@Param("userId") Integer userId);


    @Update("update user set nickname=#{nickname} where id =#{userId}")
    void updateNickname(@Param("userId") int userId,@Param("nickname") String nickname);

    @Update("update user set avatar_url=#{avatar_url} where id = #{userId}")
    void updateAvatar(@Param("userId") int userId,@Param("avatar_url") String avatar_url);

    @Update("update user set password = #{password} where id=#{userId}")
    void updatePassword(@Param("userId") int userId,@Param("password") String password);

    @Update("update user set password = #{password},salt=#{salt} where id =#{userId}")
    void updatePasswordAndSalt(@Param("userId") int userId,@Param("password")String password,@Param("salt") String salt);

    List<UserResponse> findFollower(@Param("userId") int userId);
}
