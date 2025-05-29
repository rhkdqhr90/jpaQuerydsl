package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import jakarta.persistence.PersistenceUnit;

import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;


import java.util.List;

import static com.querydsl.core.types.Projections.*;
import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

@SpringBootTest
@Transactional
public class QueryDslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30,teamB);
        Member member4 = new Member("member4", 40,teamB);
        em.persist(member);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL(){
        //member1을 찾아라
        Member singleResult = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        assertThat(singleResult.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQueryDSL(){

        Member findMember = queryFactory
                .selectFrom(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");


    }

    @Test
    public void search(){
        Member member1 = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1").and(member.age.eq(10)))
                .fetchOne();

        assertThat(member1.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam(){
        Member member1 = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne();

        assertThat(member1.getUsername()).isEqualTo("member1");
    }
    @Test
    public void resultFetch(){
//        List<Member> fetch = queryFactory
//                .selectFrom(member)
//                .fetch();
//        Member fetchOne = queryFactory
//                .selectFrom(member)
//                .fetchOne();
//        Member fetchFirst = queryFactory
//                .selectFrom(member)
//                .fetchFirst();
//        QueryResults<Member> result = queryFactory
//                .selectFrom(member)
//                .fetchResults();
//
//        result.getTotal();
//        List<Member> results = result.getResults();

        queryFactory
                .selectFrom(member)
                .fetchCount();

    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순
     * 단, 2에서 회원 이름이 없으면 마지막에 출력(null last)
     */
    @Test
    public void sort(){
        em.persist(new Member(null, 100));
        em.persist(new Member("member6", 100));
        em.persist(new Member("member7", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();
        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member membernull = result.get(2);
        assertThat(member5.getUsername()).isEqualTo("member6");
        assertThat(member6.getUsername()).isEqualTo("member7");
        assertThat(membernull.getUsername()).isNull();
    }

    @Test
    public void paging1(){
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();
        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void paging2(){
        QueryResults<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(result.getTotal()).isEqualTo(4);
        assertThat(result.getResults().size()).isEqualTo(2);
        assertThat(result.getOffset()).isEqualTo(1);
    }

    @Test
    public void aggregation(){
        List<Tuple> result = queryFactory
                .select(
                        member.username.count()
                        , member.age.sum()
                        , member.age.avg()
                        , member.age.max()
                        , member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.username.count())).isEqualTo(4L);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100L);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25.0);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀 이름과 각 팀의 평균 연령을 구해라
     * @throws Exception
     */
    @Test
    public void group() throws Exception{
        //given
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();
        //when
        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        //then
        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15.0);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35.0);

     }

    /**
     * 팀 A에 소속된 모든 회원
     */

    @Test
     public void join() {
         //given
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();
        //when
        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");

         //then
      }

      @Test
      public void theta_join(){
          //given
          em.persist(new Member("teamA", 100));
          em.persist(new Member("teamA", 100));
          //when
          List<Member> result = queryFactory
                  .select(member)
                  .from(member, team)
                  .where(member.username.eq(team.name))
                  .fetch();
          //then
          assertThat(result)
                  .extracting("username")
                  .containsExactly("teamA","teamA");
       }

    /**
     * 회원과 팀을 조인하면서, 팀 이름이 TeamA인 팀만 조인, 회원은 모두 조회(teamB = Null)
     * jpql : select m from member m left join team t on t.name = 'teamA'
     */
    @Test
    public void join_on_filtering(){
           //given
        List<Tuple> teamA = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();
        //when
            teamA.forEach(tuple -> {
                System.out.println("member, team = " + tuple);
            });
           //then
        }

    /**
     * 연관관계 없는 엔티티 외부조인
     * 회원 이름이 팀 이름과 같은 대상 외부 조인
     *
     */
    @Test
    public void join_on_no_relation() {
            //given
        em.persist(new Member("teamA", 100));
        em.persist(new Member("teamA", 100));
        em.persist(new Member("teamC", 100));
            //when
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();
        //then
        result.forEach(tuple -> {
            System.out.println("member, team = " + tuple);
        });
    }
    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoin(){
        //given
        em.flush();
        em.clear();
        //when
        Member fetchOne = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        //then
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(fetchOne.getTeam());
        assertThat(loaded).as("패치 조인 미적용").isFalse();
    }
    @Test
    public void fetchJoinUse(){
        //given
        em.flush();
        em.clear();
        //when
        Member fetchOne = queryFactory
                .selectFrom(member)
                .join(member.team,team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();
        //then
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(fetchOne.getTeam());
        assertThat(loaded).as("패치 조인 적용").isTrue();
    }

    /**
     * 나이가 가장 많은 회원 조회
     */
    @Test
    public void subQuery(){
        QMember memberSubQuery = new QMember("memberSubQuery");
        //given
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSubQuery.age.max())
                                .from(memberSubQuery)
                ))
                .fetch();
        //when
        assertThat(result).extracting("age").containsExactly(40);
        //then
     }

    /**
     * 나이가 평균 이상 회원 조회
     */
    @Test
    public void subQueryGoe(){
        QMember memberSubQuery = new QMember("memberSubQuery");
        //given
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSubQuery.age.avg())
                                .from(memberSubQuery)
                ))
                .fetch();
        //when
        assertThat(result).extracting("age").containsExactly(30,40);
        //then
    }

    /**
     *
     */
    @Test
    public void subQueryIn(){
        QMember memberSubQuery = new QMember("memberSubQuery");
        //given
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSubQuery.age)
                                .from(memberSubQuery)
                                .where(memberSubQuery.age.gt(10))
                ))
                .fetch();
        //when
        assertThat(result).extracting("age").containsExactly(20,30,40);
        //then
    }
    @Test
    public void selectSubQuery() {
        //given
        QMember memberSub= new QMember("memberSub");
        //when
        queryFactory
                .select(memberSub.username,
                        select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();
        //then
     }

     @Test
     public void basicCase() {
         List<String> fetch = queryFactory
                 .select(member.age
                         .when(10).then("열살")
                         .when(20).then("스무살")
                         .otherwise("기타"))
                 .from(member)
                 .fetch();

         fetch.forEach(System.out::println);

      }

      @Test
      public void complexCase(){

          List<String> fetch = queryFactory
                  .select(new CaseBuilder()
                          .when(member.age.between(0, 20)).then("0살에서 20살")
                          .when(member.age.between(21, 40)).then("21살 이상")
                          .otherwise("기타"))
                  .from(member)
                  .fetch();
       }

       @Test
       public void constant() throws Exception{
           //given
           List<Tuple> fetch = queryFactory
                   .select(member.username, Expressions.constant("A"))
                   .from(member)
                   .fetch();
           
           fetch.forEach(System.out::println);

       }
       
       @Test
       public void concat() {
           List<String> fetch = queryFactory
                   .select(member.username.concat("_").concat(member.age.stringValue()))
                   .from(member)
                   .where(member.username.eq("member1"))
                   .fetch();
           fetch.forEach(System.out::println);
       }

       @Test
       public void simpleProjection(){

           List<String> fetch = queryFactory
                   .select(member.username)
                   .from(member)
                   .fetch();
        }
        @Test
        public void tupleProjection(){
            List<Tuple> fetch = queryFactory
                    .select(member.username, member.age)
                    .from(member)
                    .fetch();

            fetch.forEach(tuple -> {
                String username = tuple.get(member.username);
                Integer age = tuple.get(member.age);
                System.out.println("username = " + username + ", age = " + age);
            });


        }

    @Test
    public void findDtoByJPQL() {
        List<MemberDto> resultList = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();
        resultList.forEach(System.out::println);
    }
    @Test
    public void findDtoBySetter(){
        List<MemberDto> fetch = queryFactory
                .select(bean(MemberDto.class,
                        member.username, member.age))
                .from(member)
                .fetch();
        fetch.forEach(System.out::println);

    }

    @Test
    public void findDtoByField(){
        List<MemberDto> fetch = queryFactory
                .select(fields(MemberDto.class,
                        member.username, member.age))
                .from(member)
                .fetch();
        fetch.forEach(System.out::println);
    }

    @Test
    public void findDtoByConstructor(){
        List<MemberDto> fetch = queryFactory
                .select(constructor(MemberDto.class,
                        member.username, member.age))
                .from(member)
                .fetch();
        fetch.forEach(System.out::println);

    }

    @Test
    public void findUserDto(){
        QMember qMember = new QMember("qMember");
        List<UserDto> fetch1 = queryFactory
                .select(fields(UserDto.class,
                        member.username.as("name"),
                        ExpressionUtils.as(JPAExpressions
                                .select(qMember.age.max())
                                .from(qMember),"age")
                ))
                .from(member)
                .fetch();

        fetch1.forEach(System.out::println);
    }

    @Test
    public void findUserDtoConstructor(){
        List<UserDto> fetch1 = queryFactory
                .select(constructor(UserDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        fetch1.forEach(System.out::println);
    }

    @Test
    public void findDtoByQueryProjection(){
        List<MemberDto> fetch = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();
        fetch.forEach(System.out::println);


    }


            

        

           










        



}
