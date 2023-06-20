from rest_framework.permissions import BasePermission


class IsAdmin(BasePermission):
    def has_permission(self, request, view):
        return request.user.groups.filter(name = 'Admin')
    
class IsTeamLead(BasePermission):
    def has_permission(self, request, view):
        return request.user.groups.filter(name = 'Team_Leader')

class IsUser(BasePermission):
    def has_permission(self, request, view):
        return request.user.groups.filter(name = 'User')
    # removed all .exists methods       