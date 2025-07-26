"""add organizer_name to events

Revision ID: 012_add_organizer_name_to_events
Revises: 011_add_attendance_columns_to_volunteer_events
Create Date: 2025-01-26 12:00:00.000000

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = '012_add_organizer_name_to_events'
down_revision = '011_add_attendance_columns_to_volunteer_events'
branch_labels = None
depends_on = None


def upgrade():
    # Add organizer_name column to events table
    op.add_column('events', sa.Column('organizer_name', sa.String(), nullable=True))


def downgrade():
    # Remove organizer_name column from events table
    op.drop_column('events', 'organizer_name')
