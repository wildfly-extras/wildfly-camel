<%--
  #%L
  Wildfly Camel :: Example :: Camel REST
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
    <title>WildFly Camel Subsystem REST Example</title>
    <link href="bootstrap.min.css" rel="stylesheet"/>
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js"></script>
    <script language="javascript" type="text/javascript">
      function createOrUpdateCustomer() {
        if($("#customerId").val() == '') {
          createCustomer()
        } else {
          updateCustomer()
        }
      }

      function createCustomer() {
        var firstName = $("#firstName").val()
        var lastName = $("#lastName").val()

        $.ajax({
          method: "POST",
          url: "/example-camel-rest/camel/customer",
          data: JSON.stringify({ firstName: firstName, lastName: lastName }),
          dataType: 'json',
          contentType: 'application/json'
        }).done(function() {
          reset()
        });
      }

      function updateCustomer() {
        var firstName = $("#firstName").val()
        var lastName = $("#lastName").val()
        var id = $("#customerId").val()

        $.ajax({
          method: "PUT",
          url: "/example-camel-rest/rest/customer/",
          data: JSON.stringify({ id: id, firstName: firstName, lastName: lastName }),
          contentType: 'application/json'
        }).done(function() {
          reset()
        }).fail(function() {
          alert('fail')
        });
      }

      function deleteCustomer(id) {
        $.ajax({
          method: "DELETE",
          url: "/example-camel-rest/rest/customer/" + id
        }).done(function(data) {
          reset()
        });
      }

      function getCustomer(id) {
        $.ajax({
          method: "GET",
          url: "/example-camel-rest/camel/customer/" + id
        }).done(function(data) {
          $("#firstName").val(data.firstName)
          $("#lastName").val(data.lastName)
          $("#customerId").val(data.id)
        });
      }

      function getCustomers() {
        $.getJSON(
          "/example-camel-rest/rest/customer"
        ).done(function(data) {
          var html = ''
          $("#customers").empty()
          $.each(data, function (key, value) {
            html += '<div class="row"><div class="col-md-4"><a class="submit-link" onclick="getCustomer(' + value.id + ')">' + value.firstName + ' ' + value.lastName + '</a></div>'
            html += '<div class="col-md-1">'
            html += '<button type="button" class="close" aria-label="Close" onclick="deleteCustomer(' + value.id + ')"><span aria-hidden="true">&times;</span></button>'
            html += '</div></div>'
          });
          $("#customers").html(html)
        });
      }

      function reset() {
        $('#customerForm').trigger("reset")
        $('#customerId').val("")
        getCustomers()
      }

      $(document).ready(getCustomers)
    </script>
</head>
<body>
<div class="container">
    <div class="page-header">
        <h1>Manage Customers</h1>
        <div class="row">
            <div class="col-md-2">
                <form id="customerForm">
                    <div class="form-group">
                        <label for="firstName">First Name</label>
                        <input type="text" class="form-control" id="firstName" name="firstName"/>
                    </div>
                    <div class="form-group">
                        <label for="lastName">Last Name</label>
                        <input type="text" class="form-control" id="lastName" name="lastName"/>
                    </div>
                    <div class="form-group">
                        <input type="button" class="btn btn-primary" value="Clear" onclick="reset()"/>
                        <input type="button" class="btn btn-primary" value="Submit" onclick="createOrUpdateCustomer()"/>
                    </div>
                    <input type="hidden" name="id" id="customerId" />
                </form>
            </div>
            <div class="col-md-4">
              <div id="customers"></div>
            </div>
        </div>
    </div>
</div>
</body>
</html>
