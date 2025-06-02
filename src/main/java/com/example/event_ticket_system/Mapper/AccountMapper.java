package com.example.event_ticket_system.Mapper;

import com.example.event_ticket_system.Entity.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AccountMapper {

    @Select("select email email, password_hash password_hash from users where email = #{email}")
    User findByEmail(String email);

    @Insert("INSERT INTO users(email, password_hash, full_name) VALUES(#{email}, #{password_hash}, #{fullName})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertAccount(User user);

    @Update("update users set password_hash = #{passwordHash} where email = #{email}")
    int updatePassword(User user);
}
