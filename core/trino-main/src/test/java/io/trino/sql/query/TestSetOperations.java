/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.sql.query;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestSetOperations
{
    private QueryAssertions assertions;

    @BeforeClass
    public void init()
    {
        assertions = new QueryAssertions();
    }

    @AfterClass(alwaysRun = true)
    public void teardown()
    {
        assertions.close();
        assertions = null;
    }

    @Test
    public void testExceptInSubquery()
    {
        assertThat(assertions.query(
                "WITH t(id) AS (VALUES 1, 2, 3) " +
                        "SELECT * FROM t WHERE id IN (" +
                        "    VALUES 1, 1, 2, 3" +
                        "    EXCEPT" +
                        "    VALUES 1)"))
                .matches("VALUES 2, 3");

        assertThat(assertions.query(
                "WITH t(id) AS (VALUES 1, 2, 3) " +
                        "SELECT * FROM t WHERE id IN (" +
                        "    VALUES 1, 1, 2, 2, 3" +
                        "    EXCEPT ALL" +
                        "    VALUES 1, 2, 2)"))
                .matches("VALUES 1, 3");
    }

    @Test
    public void testIntersectInSubquery()
    {
        assertThat(assertions.query(
                "WITH t(id) AS (VALUES 1, 2, 3) " +
                        "SELECT * FROM t WHERE id IN (" +
                        "    VALUES 1, 1, 2" +
                        "    INTERSECT" +
                        "    VALUES 2, 3)"))
                .matches("VALUES 2");

        assertThat(assertions.query(
                "WITH t(id) AS (VALUES 1, 2, 3) " +
                        "SELECT * FROM t WHERE id IN (" +
                        "    VALUES 1, 1, 2" +
                        "    INTERSECT ALL" +
                        "    VALUES 2, 3)"))
                .matches("VALUES 2");
    }

    @Test
    public void testUnionInSubquery()
    {
        assertThat(assertions.query(
                "WITH t(id) AS (VALUES 1, 2, 3, 4) " +
                        "SELECT * FROM t WHERE id IN (" +
                        "    VALUES 1, 2" +
                        "    UNION" +
                        "    VALUES 2, 3)"))
                .matches("VALUES 1, 2, 3");

        assertThat(assertions.query(
                "WITH t(id) AS (VALUES 1, 2, 3, 4) " +
                        "SELECT * FROM t WHERE id IN (" +
                        "    VALUES 1, 2" +
                        "    UNION ALL" +
                        "    VALUES 2, 3)"))
                .matches("VALUES 1, 2, 3");
    }

    @Test
    public void testUnionAllVsDistinctInSubquery()
    {
        // ensure that the UNION ALL and UNION DISTINCT are treated as different operations and subexpressions are not deduped
        assertThat(assertions.query(
                "SELECT (" +
                        "    SELECT array_agg(v ORDER BY v) FROM (" +
                        "        VALUES 1, 2, 3" +
                        "        UNION" +
                        "        VALUES 3, 4" +
                        "    ) t(v))," +
                        "    (" +
                        "    SELECT array_agg(v ORDER BY v) FROM (" +
                        "        VALUES 1, 2, 3" +
                        "        UNION ALL" +
                        "        VALUES 3, 4" +
                        "    ) t(v))"))
                .matches("VALUES (ARRAY[1, 2, 3, 4], ARRAY[1, 2, 3, 3, 4])");
    }

    @Test
    public void testIntersectAllVsDistinctInSubquery()
    {
        // ensure that the INTERSECT ALL and INTERSECT DISTINCT are treated as different operations and subexpressions are not deduped
        assertThat(assertions.query(
                "SELECT (" +
                        "    SELECT array_agg(v ORDER BY v) FROM (" +
                        "        VALUES 1, 2, 3, 3" +
                        "        INTERSECT" +
                        "        VALUES 2, 2, 3, 3, 4" +
                        "    ) t(v))," +
                        "    (" +
                        "    SELECT array_agg(v ORDER BY v) FROM (" +
                        "        VALUES 1, 2, 3, 3" +
                        "        INTERSECT ALL" +
                        "        VALUES 2, 2, 3, 3, 4" +
                        "    ) t(v))"))
                .matches("VALUES (ARRAY[2, 3], ARRAY[2, 3, 3])");
    }

    @Test
    public void testExceptAllVsDistinctInSubquery()
    {
        // ensure that the EXCEPT ALL and EXCEPT DISTINCT are treated as different operations and subexpressions are not deduped
        assertThat(assertions.query(
                "SELECT (" +
                        "    SELECT array_agg(v ORDER BY v) FROM (" +
                        "        VALUES 1, 1, 2, 2, 3, 3" +
                        "        EXCEPT" +
                        "        VALUES 2, 3, 3, 4" +
                        "    ) t(v))," +
                        "    (" +
                        "    SELECT array_agg(v ORDER BY v) FROM (" +
                        "        VALUES 1, 1, 2, 2, 3, 3" +
                        "        EXCEPT ALL" +
                        "        VALUES 2, 3, 3, 4" +
                        "    ) t(v))"))
                .matches("VALUES (ARRAY[1], ARRAY[1, 1, 2])");
    }
}
