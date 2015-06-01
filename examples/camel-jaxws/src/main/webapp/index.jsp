<%--
  #%L
  Wildfly Camel :: Example :: Camel JAX-WS
  %%
  Copyright (C) 2013 - 2015 RedHat
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
    <title>WildFly Camel Subsystem JAX-WS Example</title>
    <link href="bootstrap.min.css" rel="stylesheet"/>
</head>
<body>
<div class="container">
    <div class="page-header">
        <h1>Send A Greeting</h1>
        <form action="/example-camel-jaxws/jaxws" method="post">
            <div class="form-group">
                <label for="message">Message</label>
                <input type="text" class="form-control" id="message" name="message"/>
            </div>
            <div class="form-group">
                <label for="name">Name</label>
                <input type="text" class="form-control" id="name" name="name"/>
            </div>
            <div class="form-group">
                <input type="submit" class="btn btn-primary" value="Send"/>
            </div>
        </form>
    </div>
</div>
<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js"></script>
</body>
</html>
