<%--
  #%L
  Wildfly Camel :: Example :: Camel CDI
  %%
  Copyright (C) 2013 - 2014 RedHat
  %%
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  #L%
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<html>
<head>
    <title>Wildfly Camel Examples :: JPA</title>
    <style type="text/css">
        body {
            font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;
            font-size: 14px;
        }
        table {
            width: 500px;
            margin: 5px;
            padding: 2px;
            border: solid #000000 1px;
        }
        td {
            text-align: center;
        }
    </style>
    <script type="text/javascript">
        function refresh() {
            window.location.reload(true);
        }
        setTimeout(refresh, 5000);
    </script>
</head>
<body>
<table>
    <tr>
        <th>Id</th>
        <th>First Name</th>
        <th>Last Name</th>
    </tr>
    <c:forEach var="customer" items="${customers}">
    <tr>
        <td>${customer.id}</td>
        <td>${customer.firstName}</td>
        <td>${customer.lastName}</td>
    </tr>
    </c:forEach>
</table>
</body>
</html>
