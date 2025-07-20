package com.sky.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import com.sky.annotation.AutoFill;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.enumeration.OperationType;

@Mapper
public interface EmployeeMapper {

    /**
     * 根据用户名查询员工
     * @param username
     * @return
     */
    @Select("select * from employee where username = #{username}")
    Employee getByUsername(String username);

    /**
     * 插入员工信息
     * @param employee
     * 
     */
    @Insert("""
            insert into employee (name, username, password, phone, sex, id_number, status, create_time, update_time, create_user, update_user) 
            VALUES 
            (#{name}, #{username}, #{password}, #{phone}, #{sex}, #{idNumber}, #{status}, #{createTime}, #{updateTime}, #{createUser}, #{updateUser})
            """)
    @AutoFill(value= OperationType.INSERT)
    void insert(Employee employee);


	List<Employee> page(EmployeePageQueryDTO employeePageQueryDTO);
    @AutoFill(value = OperationType.UPDATE)
    void updata(Employee employee);

    @Select("""
            select id, name, username, password, phone, sex, id_number, status, create_time, update_time, create_user, update_user from employee
             where id = #{id}
            """)
    Employee getinfo(Long id);
}
