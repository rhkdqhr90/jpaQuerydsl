package study.querydsl.entity;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@SpringBootTest
@Transactional
class MemberTest {
    @Autowired
    EntityManager em;


    @Test
    public void testEntity(){
        //given
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

        em.flush();
        em.clear();
        //when
        List<Member> selectMFromMemberM = em.createQuery("select m from Member m", Member.class).getResultList();
        //then
        for(Member m : selectMFromMemberM){
            System.out.println("member = " + m);
            System.out.println("memberTeam = " + m.getTeam());
        }
     }

}